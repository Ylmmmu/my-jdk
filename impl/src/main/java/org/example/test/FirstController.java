package org.example.test;


import org.example.spring.Component;
import org.example.spring.mvc.Controller;
import org.example.spring.mvc.RequestBody;
import org.example.spring.mvc.RequestMapping;
import org.example.spring.mvc.RequestParam;

@Controller
@RequestMapping
@Component
public class FirstController {


    @RequestMapping(path = "/1first")
    public Response first(@RequestBody Request request, @RequestParam String address, String age)
    {
        System.out.println(Thread.currentThread()+"first");
        return Response.builder().name(request.getName()).address(address).age(age).build();

    }
}
