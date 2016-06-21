package at.fh.swenga.ima.controller;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.fluttercode.datafactory.impl.DataFactory;
import org.hibernate.exception.GenericJDBCException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import at.fh.swenga.ima.dao.AttachmentRepository;
import at.fh.swenga.ima.dao.ForumEntryRepository;
import at.fh.swenga.ima.model.AttachmentModel;
import at.fh.swenga.ima.model.ForumEntryModel;
import at.fh.swenga.ima.model.TaskModel;

@Controller

public class ForumEntryController {

	@Autowired
	ForumEntryRepository forumEntryRepository;

	@Autowired
	AttachmentRepository attachmentRepository;

	@RequestMapping(value = { "/forum", "list" })
	public String index(Model model, @AuthenticationPrincipal UserDetails userDetails) {
		List<ForumEntryModel> forumEntrys = forumEntryRepository.findAll();
		model.addAttribute("forumEntrys", forumEntrys);
		model.addAttribute("userName", userDetails.getUsername());
		model.addAttribute("type", "findAll");
		model.addAttribute("pageTitle", "Forum");
		

		return "forumIndex";
	}

	@RequestMapping("/fillForumEntrys")
	@Transactional
	public String fillData(Model model, @AuthenticationPrincipal UserDetails userDetails) {

		// Creates always the same data
		DataFactory df = new DataFactory();
		Date now = new Date();

		for (int i = 0; i < 10; i++) {
			ForumEntryModel fem = new ForumEntryModel(df.getFirstName(), df.getRandomText(10, 50),
					userDetails.getUsername());
			forumEntryRepository.save(fem);
		}

		return "forward:/forum";
	}

	@RequestMapping("/deleteForumEntry")
	public String deleteData(Model model, @RequestParam int id) {
		forumEntryRepository.delete(id);

		return "forward:/forum";
	}

	@RequestMapping(value = "/addForumEntry", method = RequestMethod.GET)
	public String showAddForumEntryForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
		model.addAttribute("userDetails", userDetails);
		return "forumEntryEdit";
	}

	@RequestMapping(value = "/addForumEntry", method = RequestMethod.POST)
	public String addForumEntry(@Valid @ModelAttribute ForumEntryModel newForumEntryModel, BindingResult bindingResult,
			Model model, @RequestParam("myFile") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {

		if (bindingResult.hasErrors()) {
			String errorMessage = "";
			for (FieldError fieldError : bindingResult.getFieldErrors()) {
				errorMessage += fieldError.getField() + " is invalid<br>";
			}
			// put the errors into the model
			model.addAttribute("errorMessage", errorMessage);
			return "forumEntryEdit";
		}

		ForumEntryModel forumEntry = forumEntryRepository.findForumEntryById(newForumEntryModel.getId());

		try {
			if (forumEntry != null) {
				model.addAttribute("errorMessage", "Entry already exists!<br>");
			} else {

				// Create a new document and set all available infos

				ForumEntryModel savedForumEntry = forumEntryRepository.save(newForumEntryModel);
				AttachmentModel attachment = new AttachmentModel();
				attachment.setContent(file.getBytes());
				attachment.setContentType(file.getContentType());
				attachment.setCreated(new Date());
				attachment.setFilename(file.getOriginalFilename());
				attachment.setName(file.getName());
				savedForumEntry.setUserName(userDetails.getUsername());
				savedForumEntry.setAttachment(attachment);
				forumEntryRepository.save(savedForumEntry);
				model.addAttribute("message", "New entry " + forumEntry.getTopic() + " added.");

			}
		} catch (RuntimeException ex) {

			model.addAttribute("errorMessage", "The file you uploaded was bigger than 4MB. Try another.");
			return "forward:/forum";

		}

		catch (Exception e) {

			model.addAttribute("errorMessage", "Error:" + e.getMessage());

		}

		return "forward:/forum";

	}

	@RequestMapping(value = "/editForumEntry", method = RequestMethod.GET)
	public String showEditForumEntryForm(Model model, @RequestParam int id,
			@AuthenticationPrincipal UserDetails userDetails) {

		model.addAttribute("entryId", id);

		ForumEntryModel forumEntry = forumEntryRepository.findForumEntryById(id);

		Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

		if (forumEntry == null) {

			model.addAttribute("errorMessage", "Couldn't find entry " + id);
			return "forward:/forum";

		} else if (!authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
				&& !forumEntry.getUserName().equals(userDetails.getUsername())) {
			// not an admin and not the current user
			model.addAttribute("errorMessage", "Not authorized view tasks of " + forumEntry.getUserName());
			return "forward:/forum";
		}

		else {
			model.addAttribute("forumEntrys", forumEntry);
			return "forumEntryEdit";
		}
	}

	@RequestMapping(value = "/editForumEntry", method = RequestMethod.POST)
	public String editForumEntry(@Valid @ModelAttribute ForumEntryModel editedForumEntryModel,
			BindingResult bindingResult, Model model, @RequestParam("myFile") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {
		try {
			if (bindingResult.hasErrors()) {
				String errorMessage = "";
				for (FieldError fieldError : bindingResult.getFieldErrors()) {
					errorMessage += fieldError.getField() + " is invalid<br>";
				}
				model.addAttribute("errorMessage", errorMessage);
				return "forward:/forum";
			}

			ForumEntryModel forumEntry = forumEntryRepository.findForumEntryByTopic(editedForumEntryModel.getTopic());
			Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

			
			if (forumEntry == null) {
				model.addAttribute("errorMessage", "Entry" + editedForumEntryModel.getTopic() + "does not exist!<br>");
			}
			else if (!authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")) && !forumEntry.getUserName().equals(userDetails.getUsername())) {
				// not an admin and not the current user
				model.addAttribute("errorMessage", "Not authorized edit tasks of " + forumEntry.getUserName());
			}  else if (!authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")) && !editedForumEntryModel.getUserName().equals(userDetails.getUsername())) {
				// changed owner to different user
				model.addAttribute("errorMessage", "Not authorized to change owner to " + editedForumEntryModel.getUserName());
			} 

			else {
				// student.setId(editedTaskModel.getId());
				// forumEntry.setId(editedForumEntryModel.getId());
				forumEntry.setTopic(editedForumEntryModel.getTopic());
				forumEntry.setText(editedForumEntryModel.getText());
				forumEntry.setAttachment(editedForumEntryModel.getAttachment());
				forumEntry.setUserName(editedForumEntryModel.getUserName());

				forumEntryRepository.save(forumEntry);
			}

			// Already a document available -> delete it
			if (forumEntry.getAttachment() != null) {
				attachmentRepository.delete(forumEntry.getAttachment());
				// Don't forget to remove the relationship too
				forumEntry.setAttachment(null);
			}

			// Create a new document and set all available infos

			AttachmentModel attachment = new AttachmentModel();
			attachment.setContent(file.getBytes());
			attachment.setContentType(file.getContentType());
			attachment.setCreated(new Date());
			attachment.setFilename(file.getOriginalFilename());
			attachment.setName(file.getName());
			forumEntry.setAttachment(attachment);
			forumEntryRepository.save(forumEntry);
			model.addAttribute("message", "Changed entry " + editedForumEntryModel.getTopic());

		} catch (RuntimeException ex) {
			ForumEntryModel forumEntry = forumEntryRepository.findForumEntryByTopic(editedForumEntryModel.getTopic());

			model.addAttribute("errorMessage", "The file you uploaded was bigger than 4MB. Try another.");

		}

		catch (Exception e) {

			model.addAttribute("errorMessage", "Error:" + e.getMessage());

		}

		return "forward:/forum";
	}

	@RequestMapping("/download")
	public void download(@RequestParam("attachmentId") int attachmentId, HttpServletResponse response) {
		AttachmentModel attachment = attachmentRepository.findOne(attachmentId);

		try {
			// damit man in der Browserleiste auch den Namen sieht
			response.setHeader("Content-Disposition", "inline;filename=\"" + attachment.getFilename() + "\"");
			OutputStream out = response.getOutputStream();
			// "application/octet-stream" => as Content Type, just downloads the
			// content and dont open it
			response.setContentType(attachment.getContentType()); // => opens
																	// the
			// Content
			out.write(attachment.getContent());
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@ExceptionHandler(Exception.class)
	public String handleAllException(Exception ex) {
		return "showError";
	}

}