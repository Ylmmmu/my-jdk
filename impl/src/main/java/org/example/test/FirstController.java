package org.example.test;


import org.example.spring.Component;
import org.example.spring.mvc.RequestMapping;

@Component
@RequestMapping
public class FirstController {


    @RequestMapping(path = "/1first")
    public void first()
    {
        System.out.println(Thread.currentThread()+"first");

    }
}
