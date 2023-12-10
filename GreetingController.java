package Booking.controller;

import java.sql.Time;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import Booking.model.User;
import Booking.service.BookingService;
import Booking.service.MovieService;
import Booking.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class GreetingController {

	@Autowired
	private UserService userService;

	@Autowired
	private MovieService movieService;

	@Autowired
	private BookingService bookingService;

	@GetMapping(path = { "/", "/login" })
	public String login() {
		return "login";
	}

	@PostMapping(path = { "/", "/login" })
	public RedirectView doLogin(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes,
			HttpServletResponse response) {
		boolean isUserExists = userService.isUserExists(user);
		if (!isUserExists) {
			redirectAttributes.addFlashAttribute("error", true);
			redirectAttributes.addFlashAttribute("message", "User not exists");
			return new RedirectView("/login", true);
		} else {
			Optional<User> userOptional = userService.getUser(user);
			if (userOptional.isEmpty()) {
				redirectAttributes.addFlashAttribute("error", true);
				redirectAttributes.addFlashAttribute("message", "Invalid credentials");
				return new RedirectView("/login", true);
			} else {
				Cookie cookie = new Cookie("user", userOptional.get().getEmailId());
				response.addCookie(cookie);
				return new RedirectView("/home", true);
			}
		}
	}

	@GetMapping("signup")
	public String signup() {
		return "signup";
	}

	@PostMapping("/signup")
	public String doSignUp(@ModelAttribute("user") User user, Model model) {
		boolean isUserExists = userService.isUserExists(user);
		if (isUserExists) {
			model.addAttribute("error", true);
			model.addAttribute("message", "User already exists");
		} else {
			user = userService.addNewUser(user);
			model.addAttribute("error", false);
			model.addAttribute("message", "User created successfully");
		}
		return "signup";
	}

	@GetMapping("/home")
	public String home(Model model, @CookieValue(required = false, name = "user") String email) {
		if (email == null || email.length() == 0) {
			return "redirect:/login";
		} else {
			model.addAttribute("movies", movieService.getAllMovies());
			return "home";
		}
	}

	@GetMapping("/theatre")
	public String theatre(Model model, @RequestParam(required = true, name = "movie") Integer movie,
			@CookieValue(required = false, name = "user") String email) {
		if (email == null || email.length() == 0) {
			return "redirect:/login";
		} else if (movie == null) {
			return "redirect:/home";
		} else {
			model.addAttribute("movie", movieService.getMovieTheatres(movie).get());
			return "theatre";
		}
	}

	@GetMapping("/book")
	public String book(Model model, @RequestParam(required = true, name = "movie") Integer movie,
			@CookieValue(required = false, name = "user") String email) {
		if (email == null || email.length() == 0) {
			return "redirect:/login";
		} else if (movie == null) {
			return "redirect:/home";
		} else {
			model.addAttribute("movie", movieService.getMovieTheatres(movie).get());
			return "book";
		}
	}

	@PostMapping("/ticket_book")
	public String ticketBook(Model model, @CookieValue(required = false, name = "user") String email,
			HttpServletRequest request) {
		if (email == null || email.length() == 0) {
			return "redirect:/login";
		} else {
			Optional<User> user = userService.getUserByEmail(email);
			Time time = Time.valueOf(request.getParameter("time") + ":00");
			int seat = Integer.parseInt(request.getParameter("seat"));
			int movie = Integer.parseInt(request.getParameter("movie"));
			model.addAttribute("movie", movieService.getMovieTheatres(movie).get());
			bookingService.doMovieBooking(user.get(), movieService.getMovieTheatres(movie).get(), seat, time);
			return "redirect:/orders";
		}
	}

	@GetMapping("/orders")
	public String orders(Model model, @CookieValue(required = false, name = "user") String email,
			HttpServletRequest request) {
		if (email == null || email.length() == 0) {
			return "redirect:/login";
		} else {
			Optional<User> user = userService.getUserByEmail(email);
			if (user.isPresent()) {
				model.addAttribute("orders", bookingService.getBookings(user.get()));
			}
			return "orders";
		}
	}

	@GetMapping("/logout")
	public String logout(HttpServletResponse response) {
		Cookie cookie = new Cookie("user", null);
		response.addCookie(cookie);
		return "redirect:/login";
	}

}
