# Infrastructure: Festival Goer Token Balance Persistence

**Context**
The Infrastructure module must provide the concrete persistence layer to retrieve and store festival goer token balances. This includes JPA entity mapping, database schema management, and repository implementation for the `TokenBalanceRepository` port defined in the Domain.

**Problem**
The Infrastructure module requires a JPA-based repository implementation, persistence entities, and database schema to support the Domain's `TokenBalanceRepository` port, while maintaining clean separation of concerns (Domain models remain framework-agnostic).

**Acceptance Criteria**
- [ ] `FestivalGoerEntity` JPA entity created with proper mapping
  - Contains `id`, `drinkTokens`, `snackTokens` fields
  - Mapped to database table `festival_goers`
  - Timestamps for `createdAt`, `updatedAt` tracking
- [ ] `TokenBalanceRepositoryAdapter` implements Domain's `TokenBalanceRepository` port
- [ ] `FestivalGoerJpaRepository` Spring Data repository created for database access
- [ ] `FestivalGoerEntityMapper` converts between Domain models and JPA entities
  - Method: `toDomain(FestivalGoerEntity): TokenBalance`
  - Method: `toEntity(FestivalGoer): FestivalGoerEntity`
- [ ] Database migration/schema created for `festival_goers` table
  - Columns: `id` (primary key), `drink_tokens`, `snack_tokens`, `created_at`, `updated_at`
- [ ] Exception handling: `FestivalGoerNotFoundException` when entity not found
- [ ] Integration tests verify database persistence and retrieval
- [ ] No Domain layer classes imported in persistence layer; only port interfaces used

**Implementation Plan**
1. Create `FestivalGoerEntity` (JPA entity) in `persistence` package
   - Annotate with `@Entity`, `@Table(name = "festival_goers")`
   - Properties: `id`, `drinkTokens`, `snackTokens`, `createdAt`, `updatedAt`
   - Include getters/setters or use Lombok `@Data`
   - Ensure tokens are NOT nullable and >= 0 at database level
   
2. Create `FestivalGoerJpaRepository` interface
   - Extends `JpaRepository<FestivalGoerEntity, String>`
   - Method: `findById(String id): Optional<FestivalGoerEntity>`
   
3. Implement `TokenBalanceRepositoryAdapter` in `persistence` package
   - Implements Domain port `TokenBalanceRepository`
   - Inject `FestivalGoerJpaRepository`
   - Inject `FestivalGoerEntityMapper`
   - Method: `findTokenBalanceByFestivalGoerId(FestivalGoerId): TokenBalance`
   - Throws `FestivalGoerNotFoundException` if not found
   - Maps entity to domain model using mapper
   
4. Create `FestivalGoerEntityMapper` in `persistence/mapper` package
   - Method: `toDomain(FestivalGoerEntity): TokenBalance`
   - Method: `toEntity(TokenBalance): FestivalGoerEntity`
   - Extract token values from entity and create TokenBalance Value Object
   
5. Create database migration
   - Create table `festival_goers` with proper schema
   - Add constraints for non-negative token values
   - Add timestamps for auditing
   
6. Create integration tests
   - Test entity persistence and retrieval
   - Test exception handling for non-existent ID
   - Test database constraints (non-negative tokens)

**Gherkin Scenarios**
Feature: Festival Goer Token Balance Persistence

Scenario: Save and retrieve festival goer token balance from database
  Given a FestivalGoerEntity with ID "fgv-001", 5 drink tokens, and 7 snack tokens
  When the entity is saved to the database
  And the token balance is retrieved by ID "fgv-001"
  Then the retrieved TokenBalance has 5 drink tokens
  And the retrieved TokenBalance has 7 snack tokens

Scenario: Festival goer not found in database throws exception
  Given no festival goer with ID "fgv-999" exists in the database
  When findTokenBalanceByFestivalGoerId("fgv-999") is called
  Then a FestivalGoerNotFoundException is raised

Scenario: Update festival goer token balance
  Given a FestivalGoerEntity with ID "fgv-002" and 6 drink tokens, 9 snack tokens
  When the entity is saved to the database
  And the drink tokens are updated to 3
  And the entity is persisted
  And the balance is retrieved
  Then the retrieved TokenBalance shows 3 drink tokens
  And the retrieved TokenBalance shows 9 snack tokens

Scenario: Database constraint prevents negative drink tokens
  Given an attempt to insert a FestivalGoerEntity with -1 drink tokens
  When the entity is persisted
  Then a database constraint violation exception is raised

Scenario: Timestamps are tracked automatically
  Given a FestivalGoerEntity is saved at time T1
  When the entity is retrieved
  Then the `createdAt` timestamp is set to T1
  And the `updatedAt` timestamp is set to T1
  When the entity is updated at time T2
  And the entity is retrieved
  Then the `updatedAt` timestamp is set to T2

**Notes**
- This ticket is solely responsible for Infrastructure persistence layer.
- JPA entities contain framework-specific annotations (allowed here).
- Domain models are never persisted directly; only JPA entities touch the database.
- The `TokenBalanceRepositoryAdapter` bridges Domain ports and Infrastructure JPA repositories.
- All mapping from persistence to domain occurs in the mapper layer.
- Database migration should be idempotent and reversible (Flyway/Liquibase compatible).
