package com.aifm.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class QuoteService {

    private final List<String> quotes = List.of(
            "Focus on progress, not perfection.",
            "Small steps every day lead to big results.",
            "Do the hard things first.",
            "Your future self will thank you.",
            "Discipline beats motivation.",
            "One task at a time. One win at a time."
    );

    public String getDailyQuote() {
        int index = LocalDate.now().getDayOfYear() % quotes.size();
        return quotes.get(index);
    }

}
