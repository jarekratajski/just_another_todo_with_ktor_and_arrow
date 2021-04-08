# Starter project for ktor

Yet another TodoList for kotlin ktor.

Special feature - pure code. All the business code is pure.
Side effectes are enclosed in monads (~ suspend () -> A  == IO in arrow).

The impure parts are extracted to a separate package  = framework.

Uses `kure-potlin` detekt plugin to enforce purity.

