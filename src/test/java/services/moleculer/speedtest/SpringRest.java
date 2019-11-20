package services.moleculer.speedtest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpringRest {

	@RequestMapping("/add/{a}/{b}")
    public SpringMath add(@PathVariable int a, @PathVariable int b) {
        return new SpringMath(a, b, a + b);
    }
	
}