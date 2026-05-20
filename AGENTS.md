# AGENTS.md - AuctionUET Coding Guide

This file is the mandatory context entry point for any agent or vibe coder working in the AuctionUET repo.
Read this file first before reading any directly related source files.

## Vietnamese Content & PowerShell Encoding Warning

This codebase contains Vietnamese text in comments, strings, JSON data files, and user-facing messages.
Be aware of the following when using PowerShell commands:

- **Always use `-Encoding UTF8` flag** when reading or writing files (writing files through Powershell is not recommended and should be avoid) via PowerShell (e.g., `Get-Content`, `Set-Content`, `Out-File`, `Add-Content`).
- **Avoid piping Vietnamese text through commands** that do not explicitly handle UTF-8 (e.g., `Select-String`, `findstr`) — they may corrupt or fail to match Vietnamese characters.
- **Prefer `-Raw` with explicit encoding** when reading full files: `Get-Content -Path "file.json" -Raw -Encoding UTF8`.
- When writing content back to files, use: `Set-Content -Path "file.json" -Value $content -Encoding UTF8`.
- **JSON data files** (`data/users.json`, `data/items.json`, etc.) may contain Vietnamese names and descriptions — treat them as UTF-8 at all times.
- **Search tools like `grep` (ripgrep/`rg`)** generally handle UTF-8 well, but confirm results when matching Vietnamese strings.

## Objective

AuctionUET is an online auction system written in Java 21, Maven multi-module, socket client-server, and JavaFX.
The current architecture follows a contract-first approach: both client and server share the network contract via the `auction-protocol` module.

When adding features or fixing bugs, the goals are:

- Keep the client/server contract in sync via `auction-protocol`.
- Do not put business logic in JavaFX controllers or network controllers.
- Do not recreate separate DTOs/protocols in the client or server.
- Do not break the authentication flow, authorization, JSON persistence, or realtime push.
- Read existing code before making changes; prefer existing patterns in the repo.

## Always Read Before Editing

1. `AGENTS.md` at the repo root.
2. Source files directly related to the task and their corresponding tests.

Before editing, run `git status --short` to check if there are existing changes in the worktree. Do not revert or overwrite changes that are not yours.

## Core Modules

The root Maven project has 3 modules:

- `auction-protocol`: shared contract between client and server.
- `auction-server`: socket server, domain logic, JSON persistence.
- `auction-client`: JavaFX app, FXML controllers, socket client adapters.

The parent `pom.xml` manages Java 21, JUnit 5, Gson, SLF4J, and Logback.

## General Conventions

- Use Java 21.
- Follow a style close to the Google Java Style Guide.
- **Write all code comments in Vietnamese.** Comments are acceptable only when they help clarify logic.
- Do not add new dependencies unless truly necessary.
- Do not log or expose passwords, `hashedPassword`, `passwordSalt`, full tokens, or any sensitive data.
- Do not place business logic in network controllers or JavaFX controllers.
- Do not manually parse `Map<String, Object>` when a request DTO already exists.
- Do not create duplicate DTOs/protocols in `auction-client` or `auction-server`.
- Do not use `System.out.println`/`printStackTrace` for new server code; use `AppLogger` and `GlobalExceptionHandler`.
- On the client, do not block the JavaFX Application Thread with network calls; run in a background thread and update the UI via `Platform.runLater`.

## Contract-First Protocol

The single source of truth for the network protocol lives in `auction-protocol/src/main/java/com/auctionuet/protocol`.

Key files:

- `ActionType.java`: list of actions the client sends to the server.
- `Request.java`: `{ action, data, token }`, with `fromDto(...)` and `getDataAs(...)`.
- `Response.java`: `{ type, status, event, message, data }`, with `getDataAs(...)` and `getDataListAs(...)`.
- `MessageSerializer.java`: serialize/deserialize request/response.
- `PushMessage.java`, `PushActionType.java`, `dto/push/PushEvents.java`: realtime push.
- `dto/request/**`: DTOs for requests.
- `dto/response/**`: DTOs for responses.
- `enums/**`: shared enums like `UserRole`, `Permission`, `ItemType`, `AuctionStatus`.
- `util/NetworkGson.java`: Gson for networking, with `LocalDateTime` adapter.

Important request DTOs should implement `ValidatableDTO`. `Request.fromDto(...)` validates before sending; `Request.getDataAs(...)` re-validates after the server deserializes.

Do not create the following legacy packages:

- `auction-client/src/main/java/com/auctionuet/client/network/protocol`
- `auction-client/src/main/java/com/auctionuet/client/model/*DTO.java`
- `auction-server/src/main/java/com/auctionuet/server/network/dto`
- `auction-server/src/main/java/com/auctionuet/server/mapper` for old DTO mapping

`auction-client/src/main/java/com/auctionuet/client/model/ClientSession.java` remains a valid model because it holds the client-side token and current user.

## Server Architecture

Server entrypoints:

- `auction-server/src/main/java/com/auctionuet/server/ServerApp.java`
- `auction-server/src/main/java/com/auctionuet/server/network/server/AuctionServer.java`

Current server request flow:

1. `AuctionServer` initializes DAOs, services, controllers, and `RequestRouter`.
2. Each socket client is handled by a `ClientHandler`.
3. `ClientHandler` reads raw JSON, deserializes via `MessageSerializer`, and calls `RequestRouter`.
4. `RequestRouter` routes by `ActionType`.
5. The network controller parses the DTO, validates token/permission, and calls the service.
6. The service processes business logic, reads/writes DAO/schema as needed, and returns the protocol response DTO.
7. `GlobalExceptionHandler` converts exceptions into `Response.error(...)`.

Server layers:

- `network/server`: socket lifecycle, request routing, exception boundary.
- `network/controller`: receives request, thin auth/permission check, parses DTO, calls service, returns `Response`.
- `domain/service`: main business logic and mapping to response DTOs.
- `domain/model`: runtime in-memory models like `User`, `LiveAuction`, `BidRecord`.
- `domain/manager`: singleton managers like `DataManager`, `SessionManager`, `AuctionManager`.
- `persistence/schema`: objects persisted to JSON.
- `persistence/dao`: reads/writes JSON via `JsonFileHelper`.
- `exception`: business exceptions.
- `util`: validation, password, ID, logging, JSON helpers.

## Controllers And Services

Network controllers should only:

- `request.getDataAs(SomeRequestDTO.class)`.
- `SessionManager.getInstance().validateToken(request.getToken())` if the action requires login.
- Check `user.hasPermission(Permission.X)` if the action requires a specific permission.
- Call the service.
- Return `Response.ok(...)` or let the exception propagate to `GlobalExceptionHandler`.

Services are responsible for:

- Business rules.
- Ownership/status checks.
- Writing critical data via DAO immediately upon change.
- Creating or updating schema/domain objects.
- Mapping schema/domain to shared response DTOs in `auction-protocol`.
- Services should cooperate with each other — before implementing a business operation, check whether another service already supports it rather than reimplementing the logic independently.

Do not put business logic such as deposit calculation, auction lifecycle, auto-bid, or item ownership checks in controllers.

## Auth And Permission

Runtime user classes:

- `domain/model/User.java`
- `Bidder.java`
- `Seller.java`
- `Admin.java`

Each role overrides `hasPermission(Permission action)`.

Token/session lives in `domain/manager/SessionManager.java` and uses `ConcurrentHashMap`.

Rules:

- Actions requiring login must validate the token.
- Actions requiring a permission must check `Permission`.
- Password/hash/salt belong only to `UserSchema`; they must not appear in the runtime `User` or response DTOs.
- Do not allow `ADMIN` registration through the normal register flow.

## Persistence And Schema

Persistence uses JSON files:

- Root data seeds: `data/users.json`, `data/items.json`, `data/auctions.json`, `data/bids.json`.
- The server runtime also uses `auction-server/data/**` in the current repo.
- Tests may use `data/test_*.json` files.

DAO pattern:

- DAOs implement `GenericDAO<T extends BaseSchema>`.
- DAOs default to files in `data/*.json`.
- DAOs have a constructor accepting `filePath` for use in tests.
- DAOs currently read the full list, mutate in RAM, then write the full file back; this is an acceptable choice for a coursework project.

Schema rules:

- `BaseSchema` empty constructor has no side effects so Gson can deserialize safely.
- When creating a new entity, explicitly pass id/time using `IdGenerator.generate()` and `LocalDateTime.now()`.
- `update(...)` should update `updatedAt`.
- Critical data such as bids, wallet balances, and auction status must be written through to JSON immediately.

Items have been refactored to a flat schema:

- `ItemSchema` has `ItemType type`, `ItemCondition condition`, `Map<String, Object> extraFields`.
- `ItemType.normalizeAndValidateExtraFields(...)` is where type-specific field validation/normalization happens.
- Do not recreate the class hierarchy `ElectronicsSchema`, `ArtSchema`, `VehicleSchema` unless there is a very clear requirement.

## Auction, Bid, Wallet, Realtime

Key classes:

- `AuctionService`: create/start/end/pay auction, load running auctions, status/payment flow.
- `BidService`: place bid, bid history, auto-bid, freeze deposit.
- `WalletService`: balance/frozenBalance, deposit/withdraw/freeze/unfreeze/forfeit.
- `AuctionManager`: manages `LiveAuction` instances running in RAM, scheduler for auto-end.
- `LiveAuction`: lock bidding, auto-bid resolution, anti-sniping, observer notification.
- `ClientHandler`: implements `AuctionObserver`, sends `PushMessage` to the client.

Concurrency rules:

- Do not remove existing locks/synchronized blocks without fully understanding the bidding flow.
- `LiveAuction` uses locks and thread-safe collections for observer/deposit state.
- `BidService.placeBid(...)` and `setAutoBid(...)` are synchronized to protect the bid/deposit flow.
- `WalletService` has synchronized methods for balance mutations.

Realtime push:

- Server sends `PushMessage` with `type = "PUSH"`.
- The client `ServerConnection` listener distinguishes `"PUSH"` from normal responses.
- Push DTOs live in `auction-protocol/dto/push/PushEvents.java`.
- When adding a push event, update `PushActionType`, `PushEvents`, the server sender, and the client listener.

## Client Architecture

Client JavaFX code lives in:

- `auction-client/src/main/java/com/auctionuet/client/ClientApp.java`
- `auction-client/src/main/java/com/auctionuet/client/view/**`
- `auction-client/src/main/java/com/auctionuet/client/network/**`
- `auction-client/src/main/resources/fxml/**`
- `auction-client/src/main/resources/css/**`

Notable singletons:

- `SceneManager`: switches scene/root FXML.
- `ThemeManager`: applies light/dark CSS.
- `ClientSession`: holds the token and current `UserDTO`.
- `ServerConnection`: socket, send request, listener thread for push.

JavaFX rules:

- Each FXML must declare the correct `fx:controller`.
- `fx:id` in FXML must match the `@FXML` field in the controller.
- Network calls run in a background thread.
- UI updates run in `Platform.runLater`.
- When adding a new view, update the FXML, controller, navigation in `SceneManager`/related controller, and CSS if needed.
- Do not create separate DTOs in the client; import from `com.auctionuet.protocol.dto...`.

Client network adapter pattern:

- `AuthClient`, `ItemClient`, `AuctionClient`, `BidClient`, `WalletClient` build request DTOs, call `ServerConnection.sendRequest(...)`, and unwrap `Response`.
- When response `status != "OK"`, throw an exception with `response.getMessage()`.
- When refactoring, prefer creating generic helpers to reduce repetition while keeping public methods easy to use from JavaFX controllers.

## Adding A New Action

Minimum checklist when adding a request/response action:

1. Read a similar existing action.
2. Add or update `ActionType`.
3. Add a request DTO in `auction-protocol/dto/request/<domain>` if the action has data.
4. Request DTOs should implement `ValidatableDTO`.
5. Add a response DTO in `auction-protocol/dto/response/<domain>` if the response cannot reuse an existing DTO.
6. Add enum/shared types to `auction-protocol/enums` if both client and server use them.
7. Server: add a thin controller method in `network/controller`.
8. Server: add or update the service/domain/DAO/schema in the correct layer.
9. Server: route the action in the existing `RequestRouter`.
10. Server: ensure token and `Permission` are checked if required.
11. Client: add a method to the appropriate network adapter.
12. Client: update the JavaFX controller/FXML if the feature has a UI.
13. Tests: add unit/integration tests for the main rule and at least one error case.
14. Run the relevant tests.

Note: `ActionType` currently has some actions that are not fully routed/implemented, such as `GET_PROFILE`, `UPDATE_PROFILE`, `UPDATE_ITEM`, `DELETE_ITEM`, `GET_ALL_USERS`, `DELETE_USER`, `UPDATE_ROLE`. If a task touches these actions, check the route/controller/service/client/test instead of assuming the feature is complete just because the enum entry exists.

## Fixing Bugs

When fixing a bug:

1. Reproduce the bug by closely following the user's description. If the description is vague, think of ways to help the user debug further based on what exists. Do not write tests, fix code, or add log lines until the bug is reproduced.
2. Read files in the layer causing the bug and the adjacent layers above/below.
3. Fix the root cause, not just the symptom in the UI.
4. If the bug is in the contract, check `auction-protocol`, the client adapter, and the server controller.
5. If the bug is in persistence, check the schema, DAO, data files, and tests using a custom file path.
6. If the bug is in bidding/realtime, check `LiveAuction`, `BidService`, `AuctionManager`, `ClientHandler`, and `ServerConnection`.
7. Add or update regression tests if appropriate.

## Logging And Error Handling

Server:

- Use `AppLogger` for lifecycle/request/service/exception logging.
- `GlobalExceptionHandler` is the boundary that converts exceptions into responses.
- Business exceptions log at WARN and return a clear message.
- Unexpected exceptions log at ERROR and return `Response.error(...)`.
- Mask sensitive fields before logging.

Client:

- Prefer displaying errors in a label/dialog appropriate to the view.
- Do not let exceptions from background threads crash the UI.
- For cleanup such as unsubscribe, failure may be ignored if navigation should not be blocked — but only when there is a clear reason.

## Definition Of Done

A change is considered done when:

- Code compiles or relevant tests have been run, with a clear reason if they could not be run.
- The client/server contract is not broken.
- Auth/permission is handled correctly.
- DTOs/protocols are not duplicated outside `auction-protocol`.
- Business logic lives in service/domain, not in UI/network controllers.
- Critical persistence is written to JSON at the right time.
- The UI does not block the JavaFX thread.
- Sensitive data is not returned in responses or logs.
- New and existing tests reflect important behavior.
- Documentation or comments are updated if the change affects how the system is used.
