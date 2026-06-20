# Infrastructure: Festival Goer Token Balance Persistence

**Context**
The Infrastructure module must provide the concrete persistence layer to retrieve and store festival goer token balances. This includes JPA entity mapping, database schema management, and repository implementation for the `TokenBalanceRepository` port defined in the Domain.

**Problem**
The Infrastructure module requires a JPA-based repository implementation, persistence entities, and database schema to support the Domain's `TokenBalanceRepository` port, while maintaining clean separation of concerns (Domain models remain framework-agnostic).

**Acceptance Criteria**
- [ ] `FestivalGoerEntity` JPA entity created with proper mapping
  - Contains `id`, `totalDrinkTokens`, `totalSnackTokens`, `reservedDrinkTokens`, `reservedSnackTokens` fields
  - Mapped to database table `festival_goers`
  - Timestamps for `createdAt`, `updatedAt` tracking
- [ ] `TokenBalanceRepositoryAdapter` implements Domain's `TokenBalanceRepository` port
  - Methods: `findTokenBalanceByFestivalGoerId`, `saveTokenBalance`
- [ ] `FestivalGoerJpaRepository` Spring Data repository created for database access
- [ ] `FestivalGoerEntityMapper` converts between Domain models and JPA entities
  - Method: `toDomain(FestivalGoerEntity): TokenBalance`
  - Method: `toEntity(FestivalGoer, TokenBalance): FestivalGoerEntity`
- [ ] Database migration/schema created for `festival_goers` table
  - Columns: `id` (primary key), `total_drink_tokens`, `total_snack_tokens`, `reserved_drink_tokens`, `reserved_snack_tokens`, `created_at`, `updated_at`
  - Constraints: reserved tokens <= total tokens (check constraint)
- [ ] Exception handling: `FestivalGoerNotFoundException` when entity not found
- [ ] Integration tests verify database persistence and retrieval with reservations
- [ ] No Domain layer classes imported in persistence layer; only port interfaces used

**Implementation Plan**
1. Create `FestivalGoerEntity` (JPA entity) in `persistence` package
   - Annotate with `@Entity`, `@Table(name = "festival_goers")`
   - Properties: 
     - `id`, `totalDrinkTokens`, `totalSnackTokens`, `reservedDrinkTokens`, `reservedSnackTokens`
     - `createdAt`, `updatedAt`
   - Include getters/setters or use Lombok `@Data`
   - Ensure all token fields >= 0 and reserved <= total at entity level
   
2. Create `FestivalGoerJpaRepository` interface
   - Extends `JpaRepository<FestivalGoerEntity, String>`
   - Method: `findById(String id): Optional<FestivalGoerEntity>`
   
3. Implement `TokenBalanceRepositoryAdapter` in `persistence` package
   - Implements Domain port `TokenBalanceRepository`
   - Inject `FestivalGoerJpaRepository`, `FestivalGoerEntityMapper`
   - Method: `findTokenBalanceByFestivalGoerId(FestivalGoerId): TokenBalance`
     - Loads entity by ID
     - Maps to TokenBalance with all 6 token values (total, reserved, available)
     - Throws `FestivalGoerNotFoundException` if not found
   - Method: `saveTokenBalance(FestivalGoerId, TokenBalance): void`
     - Loads entity by ID
     - Updates: totalDrinkTokens, totalSnackTokens, reservedDrinkTokens, reservedSnackTokens
     - Persists changes
   
4. Create `FestivalGoerEntityMapper` in `persistence/mapper` package
   - Method: `toDomain(FestivalGoerEntity): TokenBalance`
     - Extracts all token values (total, reserved)
     - Creates TokenBalance Value Object with all 6 properties
   - Method: `toEntity(FestivalGoerId, TokenBalance): FestivalGoerEntity`
     - Creates/updates entity with all token values from TokenBalance
   
5. Create database migration
   - Create table `festival_goers` with schema:
     ```sql
     CREATE TABLE festival_goers (
       id VARCHAR(50) PRIMARY KEY,
       total_drink_tokens INT NOT NULL,
       total_snack_tokens INT NOT NULL,
       reserved_drink_tokens INT NOT NULL DEFAULT 0,
       reserved_snack_tokens INT NOT NULL DEFAULT 0,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CHECK (reserved_drink_tokens >= 0 AND reserved_drink_tokens <= total_drink_tokens),
       CHECK (reserved_snack_tokens >= 0 AND reserved_snack_tokens <= total_snack_tokens)
     );
     ```
   - Add indexes for query performance
   
6. Create integration tests
   - Test entity persistence and retrieval with all token values
   - Test with no reservations
   - Test with partial reservations
   - Test with full reservations
   - Test exception handling for non-existent ID
   - Test database constraints (reserved <= total)
   - Test updates to reservation values

**Gherkin Scenarios**
Feature: Festival Goer Token Balance Persistence (with Reservations)

Scenario: Save and retrieve festival goer balance with no reservations
  Given a FestivalGoerEntity with ID "fgv-001"
  And totalDrinkTokens: 6, reservedDrinkTokens: 0
  And totalSnackTokens: 9, reservedSnackTokens: 0
  When the entity is saved to the database
  And the token balance is retrieved by ID "fgv-001"
  Then the retrieved TokenBalance has:
    | Type | Total | Reserved | Available |
    | Drink | 6 | 0 | 6 |
    | Snack | 9 | 0 | 9 |

Scenario: Save and retrieve festival goer balance with partial reservations
  Given a FestivalGoerEntity with ID "fgv-001"
  And totalDrinkTokens: 6, reservedDrinkTokens: 2
  And totalSnackTokens: 9, reservedSnackTokens: 3
  When the entity is saved to the database
  And the token balance is retrieved by ID "fgv-001"
  Then the retrieved TokenBalance has:
    | Type | Total | Reserved | Available |
    | Drink | 6 | 2 | 4 |
    | Snack | 9 | 3 | 6 |

Scenario: Update festival goer reserved tokens
  Given a FestivalGoerEntity with ID "fgv-001" and reservedDrinkTokens: 0
  When the entity is saved to the database
  And reservedDrinkTokens is updated to 3
  And the entity is persisted
  And the balance is retrieved
  Then the retrieved TokenBalance shows reservedDrinkTokens: 3
  And availableDrinkTokens: 3

Scenario: Festival goer not found in database throws exception
  Given no festival goer with ID "fgv-999" exists in the database
  When findTokenBalanceByFestivalGoerId("fgv-999") is called
  Then a FestivalGoerNotFoundException is raised

Scenario: Database constraint prevents reserved exceeding total
  Given an attempt to insert a FestivalGoerEntity with totalDrinkTokens: 5 and reservedDrinkTokens: 10
  When the entity is persisted
  Then a database check constraint violation exception is raised

Scenario: Database constraint prevents negative reserved tokens
  Given an attempt to insert a FestivalGoerEntity with reservedDrinkTokens: -1
  When the entity is persisted
  Then a database constraint violation exception is raised

Scenario: Timestamps are tracked automatically
  Given a FestivalGoerEntity is saved at time T1
  When the entity is retrieved
  Then the `createdAt` timestamp is set to approximately T1
  And the `updatedAt` timestamp is set to approximately T1
  When the entity's reservedDrinkTokens is updated at time T2
  And the entity is saved
  And the entity is retrieved
  Then the `createdAt` timestamp remains unchanged
  And the `updatedAt` timestamp is updated to approximately T2

**Notes**
- This ticket is solely responsible for Infrastructure persistence layer.
- JPA entities contain framework-specific annotations (allowed here).
- Domain models are never persisted directly; only JPA entities touch the database.
- The `TokenBalanceRepositoryAdapter` bridges Domain ports and Infrastructure JPA repositories.
- All mapping from persistence to domain occurs in the mapper layer.
- Database migration should be idempotent and reversible (Flyway/Liquibase compatible).
