package company

import domain.{RepositoryError, RepositoryNotFoundError, RepositorySystemError}

import scala.util.Try

sealed trait ApplicationState

case class ParseError(cause: Throwable) extends ApplicationState
case class RetryError(cause: Throwable = null) extends ApplicationState
case class SkipError(cause: Throwable = null) extends ApplicationState

object ApplicationErrorConverters {

  implicit class Try2ApplicationError[E](val t: Try[E]) extends AnyVal {

    def toRetryError: Either[RetryError, E] =
      t.fold(
        e => Left(RetryError(e)),
        Right(_)
      )

    def toParseError: Either[ParseError, E] =
      t.fold(
        e => Left(ParseError(e)),
        Right(_)
      )
  }

  implicit class EitherRepositoryError2ApplicationError[E](
      val e: Either[RepositoryError, E])
      extends AnyVal {
    def toApplicationError: Either[ApplicationState, E] =
      e.fold(
        {
          case RepositoryNotFoundError() => Left(SkipError())
          case RepositorySystemError(t)  => Left(RetryError(t))
          case _                         => Left(RetryError())
        },
        Right(_)
      )
  }
}
