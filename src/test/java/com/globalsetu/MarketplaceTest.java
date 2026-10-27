package com.globalsetu;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketplaceTest {
    final RequestRepository requests = mock(RequestRepository.class);
    final QuoteRepository quotes = mock(QuoteRepository.class);
    final Marketplace api = new Marketplace(requests, quotes);

    @Test void expiredQuoteCannotBeAccepted() {
        Quote quote = offeredQuote();
        quote.validUntil = LocalDate.now().minusDays(1);
        assertThrows(ResponseStatusException.class,
            () -> api.decide(quote.id, new Marketplace.Decision("ACCEPT")));
        verify(quotes, never()).save(any());
        verify(requests, never()).save(any());
    }

    @Test void approvalRecordsDecisionAndUpdatesRequest() {
        Quote quote = offeredQuote();
        when(quotes.save(quote)).thenReturn(quote);
        Quote accepted = api.decide(quote.id, new Marketplace.Decision("ACCEPT"));
        assertEquals("ACCEPTED", accepted.status);
        assertNotNull(accepted.decidedAt);
        verify(requests).save(argThat(r -> r.status.equals("APPROVED")));
    }

    @Test void repeatedDecisionIsRejected() {
        Quote quote = offeredQuote();
        api.decide(quote.id, new Marketplace.Decision("ACCEPT"));
        assertThrows(ResponseStatusException.class,
            () -> api.decide(quote.id, new Marketplace.Decision("ACCEPT")));
        verify(quotes, times(1)).save(quote);
    }

    @Test void unsupportedDecisionDoesNotSave() {
        Quote quote = offeredQuote();
        assertThrows(ResponseStatusException.class,
            () -> api.decide(quote.id, new Marketplace.Decision("CONFIRM")));
        verify(quotes, never()).save(any());
    }

    private Quote offeredQuote() {
        ServiceRequest request = new ServiceRequest();
        request.id = UUID.randomUUID();
        request.status = "AWAITING_CUSTOMER_DECISION";
        Quote quote = new Quote();
        quote.id = UUID.randomUUID();
        quote.requestId = request.id;
        quote.validUntil = LocalDate.now().plusDays(5);
        when(requests.findById(request.id)).thenReturn(Optional.of(request));
        when(quotes.findById(quote.id)).thenReturn(Optional.of(quote));
        return quote;
    }
}
