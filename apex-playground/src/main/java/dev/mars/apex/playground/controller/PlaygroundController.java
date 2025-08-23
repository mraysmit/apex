package dev.mars.apex.playground.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Main web controller for the APEX Playground interface.
 * 
 * Provides the main playground interface with 4-panel JSFiddle-style layout
 * for interactive APEX rules engine testing and experimentation.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Controller
@RequestMapping("/playground")
public class PlaygroundController {

    private static final Logger logger = LoggerFactory.getLogger(PlaygroundController.class);

    /**
     * Display the main playground interface.
     * 
     * @param model Spring MVC model for template rendering
     * @return The playground template name
     */
    @GetMapping({"", "/"})
    public String playground(Model model) {
        logger.info("Loading APEX Playground interface");
        
        // Add basic model attributes for the playground
        model.addAttribute("title", "APEX Playground");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("apiBaseUrl", "/playground/api");
        
        return "playground";
    }

    /**
     * Display the help/documentation page.
     * 
     * @param model Spring MVC model for template rendering
     * @return The help template name
     */
    @GetMapping("/help")
    public String help(Model model) {
        logger.info("Loading playground help page");
        
        model.addAttribute("title", "APEX Playground - Help");
        
        return "help";
    }

    /**
     * Display the examples page.
     * 
     * @param model Spring MVC model for template rendering
     * @return The examples template name
     */
    @GetMapping("/examples")
    public String examples(Model model) {
        logger.info("Loading playground examples page");
        
        model.addAttribute("title", "APEX Playground - Examples");
        
        return "examples";
    }
}
