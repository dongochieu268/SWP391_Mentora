package com.edunac.mentora.controller.lecturer;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(basePackages = "com.edunac.mentora.controller.lecturer")
public class LecturerExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleLecturerException(RuntimeException exception,
                                           RedirectAttributes redirectAttributes) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "Không thể hoàn thành thao tác.";
        }

        redirectAttributes.addFlashAttribute("error", message);
        return "redirect:/lecturer/classes";
    }
}
