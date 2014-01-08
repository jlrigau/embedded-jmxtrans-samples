/*
 * Copyright (c) 2010-2013 the original author or authors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package org.jmxtrans.embedded.samples.cocktail.cocktail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
@ManagedResource("cocktail:type=CocktailController,name=CocktailController")
@Controller
public class CocktailController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicInteger displayedHomeCount = new AtomicInteger();

    @Autowired
    private CocktailRepository cocktailRepository;

    @ManagedMetric(metricType = MetricType.GAUGE)
    public int getDisplayedHomeCount() {
        return displayedHomeCount.get();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home() {
        displayedHomeCount.incrementAndGet();

        return "welcome";
    }

    @RequestMapping(value = "/cocktail/{id}/comment", method = RequestMethod.POST)
    public String addComment(@PathVariable long id, @RequestParam("comment") String comment, HttpServletRequest request) {

        Cocktail cocktail = cocktailRepository.get(id);
        if (cocktail == null) {
            throw new CocktailNotFoundException(id);
        }
        logger.debug("Add comment: '{}' to {}", comment, cocktail);
        cocktail.getComments().addFirst(comment);
        cocktailRepository.update(cocktail);

        return "redirect:/cocktail/{id}";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/cocktail/suggest/ingredient")
    @ResponseBody
    public List<String> suggestCocktailIngredientWord(@RequestParam("term") String term) {
        List<String> words = this.cocktailRepository.suggestCocktailIngredientWords(term);
        logger.trace("autocomplete word for {}:{}", term, words);
        return words;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/cocktail/suggest/name")
    @ResponseBody
    public List<String> suggestCocktailNameWord(@RequestParam("term") String term) {
        List<String> words = this.cocktailRepository.suggestCocktailNameWords(term);
        logger.trace("autocomplete word for {}:{}", term, words);
        return words;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/cocktail/{id}")
    public String view(@PathVariable long id, Model model) {
        Cocktail cocktail = cocktailRepository.get(id);
        if (cocktail == null) {
            throw new CocktailNotFoundException(id);
        }
        model.addAttribute(cocktail);

        return "cocktail/view";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/cocktail")
    public ModelAndView find(@RequestParam(value = "name", required = false) String name,
                             @RequestParam(value = "ingredient", required = false) String ingredient) {

        Collection<Cocktail> cocktails = cocktailRepository.find(ingredient, name);

        return new ModelAndView("cocktail/view-all", "cocktails", cocktails);
    }

    /**
     * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
     */
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public static class CocktailNotFoundException extends RuntimeException {

        private long id;

        public CocktailNotFoundException(long id) {
            super("Resource: " + id);
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }
}
