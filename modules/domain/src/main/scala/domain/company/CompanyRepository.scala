package domain.company

import domain.RepositoryError

trait CompanyRepository {
  def get(id: CompanyId): Either[RepositoryError, Company]
  def gets: Either[RepositoryError, Set[Company]]
  def findAll(): Either[RepositoryError, Option[Set[Company]]]
  def insertInternal(company: Company): Either[RepositoryError, Company]
}
