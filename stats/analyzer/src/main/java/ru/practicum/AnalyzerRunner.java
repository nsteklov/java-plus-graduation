package ru.practicum;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.service.EventSimilarityProcessor;
import ru.practicum.service.UserActionProcessor;

@Component
public class AnalyzerRunner implements CommandLineRunner {
    private final UserActionProcessor userActionProcessor;
    private final EventSimilarityProcessor eventSimilarityProcessor;

    public AnalyzerRunner(UserActionProcessor userActionProcessor, EventSimilarityProcessor eventSimilarityProcessor) {
        this.userActionProcessor = userActionProcessor;
        this.eventSimilarityProcessor = eventSimilarityProcessor;
    }

    @Override
    public void run(String... args) throws Exception {
        // запускаем в отдельном потоке обработчик событий действий пользователей
        Thread userActionThread = new Thread(userActionProcessor);
        userActionThread.setName("UserActionHandlerThread");
        userActionThread.start();

        // В текущем потоке начинаем обработку
        // событий расчета сходств событий
        eventSimilarityProcessor.start();
    }
}
