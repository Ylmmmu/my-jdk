package org.example.test;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Response {

    private String name;

    private String age;

    private String address;
}
