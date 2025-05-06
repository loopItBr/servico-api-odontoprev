package com.odontoPrev.odontoPrev.domain.service;


import org.springframework.stereotype.Component;

@Component
public class Tarefa {
    public void executarLogica(){
        System.out.println("Executando a tarefa em: " + java.time.LocalDateTime.now());
    }
}
