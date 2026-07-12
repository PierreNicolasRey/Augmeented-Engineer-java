package com.exalt.it.belair.domain.order.ports.out;

/**
 * Outbound port for publishing domain events.
 * Abstracts the event publishing mechanism (message broker, event bus, etc.) from the domain layer.
 * Implementation to be provided by Infrastructure layer adapters.
 */
public interface IEventPublisher {
    
    /**
     * Publishes a domain event.
     * 
     * @param event the event to publish
     */
    void publish(Object event);
}
