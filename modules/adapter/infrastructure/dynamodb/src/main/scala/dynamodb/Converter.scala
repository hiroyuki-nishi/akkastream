package dynamodb

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import domain.{
  RepositoryError,
  RepositoryNotFoundError,
  RepositoryOptimisticError,
  RepositorySystemError
}

import scala.util.Try

object Converter {
  implicit class Try2Error[E](val t: Try[E]) extends AnyVal {
    def toRepositoryError: Either[RepositoryError, E] =
      t.fold(
        {
          case _: ConditionalCheckFailedException =>
            Left(RepositoryOptimisticError())
          case _: IndexOutOfBoundsException => Left(RepositoryNotFoundError())
          case e                            => Left(RepositorySystemError(e))
        },
        Right(_)
      )
  }
}
