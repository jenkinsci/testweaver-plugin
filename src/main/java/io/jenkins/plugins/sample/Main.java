package io.jenkins.plugins.sample;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        String path = "dfg";
        if(new File(path).isAbsolute()==false)
            path="\""+new File("asd",path)+"\"";
        System.out.println(path);
    }
}
