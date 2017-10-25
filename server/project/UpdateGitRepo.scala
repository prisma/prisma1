import play.api.libs.json.{JsSuccess, JsValue, Json}

import scalaj.http.{Base64, Http, HttpRequest}

object GithubClient {
  def apply(): GithubClient = GithubClient(Env.read("GITHUB_ACCESS_TOKEN"))
}

case class GithubClient(accessToken: String) {
  import JsonFormatting._

  val host       = "https://api.github.com"
  val authHeader = "Authorization" -> s"token $accessToken"

  def updateFile(owner: String, repo: String, filePath: String, newContent: String, branch: String): Unit = {
    getCurrentSha(owner, repo, filePath, branch) match {
      case Some(currentSha) =>
        updateContentsOfFile(owner, repo, filePath, currentSha, newContent, branch)
        println(s"Updated file $filePath in other repo successfully.")
      case None =>
        println(s"Branch $branch in other repo does not seem to exist. Won't update file.")
    }
  }

  def getCurrentSha(owner: String, repo: String, filePath: String, branch: String): Option[String] = {
    val request = baseRequest(urlPath(owner, repo, filePath, branch))
    request.asJson(200, 404).validate[GetContentResponse](getContentReads) match {
      case JsSuccess(parsed, _) => Some(parsed.sha)
      case _                    => None
    }
  }

  def updateContentsOfFile(owner: String, repo: String, filePath: String, sha: String, newContent: String, branch: String): JsValue = {
    val request = baseRequest(urlPath(owner, repo, filePath))
    val payload = UpdateContentsRequest(
      message = s"Updated by the SBT Task in the open source repo to: $newContent",
      content = Base64.encodeString(newContent),
      sha = sha,
      branch = branch
    )
    request.put(Json.toJson(payload)(updateContentsWrites).toString).asJson(200)
  }

  def urlPath(owner: String, repo: String, filePath: String, branch: String): String = urlPath(owner, repo, filePath) + s"?ref=$branch"
  def urlPath(owner: String, repo: String, filePath: String): String                 = s"/repos/$owner/$repo/contents/$filePath"
  def baseRequest(path: String)                                                      = Http(s"$host$path").headers(authHeader).header("content-type", "application/json")

  implicit class HttpRequestExtensions(httpRequest: HttpRequest) {
    def asJson(allowedStatusCodes: Int*): JsValue = {
      val response          = httpRequest.asString
      val isAllowedResponse = allowedStatusCodes.contains(response.code)
      require(isAllowedResponse, s"The request did not result in an expected status code. Allowed status are $allowedStatusCodes. The response was: $response")
      Json.parse(response.body)
    }
  }
}

object JsonFormatting {
  import play.api.libs.json._

  case class GetContentResponse(sha: String)
  case class UpdateContentsRequest(message: String, content: String, sha: String, branch: String)

  implicit val getContentReads      = Json.reads[GetContentResponse]
  implicit val updateContentsWrites = Json.writes[UpdateContentsRequest]
}

object Env {
  def read(name: String) = sys.env.getOrElse(name, sys.error(s"Env var $name must be set"))
}
