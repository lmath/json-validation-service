package validator

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonCleanerTest extends AnyFlatSpec with Matchers {

  it should "remove nulls from a json" in {
    val input =
      """
        | {
        |   "name": "Roxie",
        |   "breed": "Rottie/lab cross",
        |   "birth_year": null
        | }
      """.stripMargin

    val output = "{\"name\":\"Roxie\",\"breed\":\"Rottie/lab cross\"}" // the validation deals in strings so we'll test in strings for now

    JsonCleaner.clean(input) shouldBe output
  }


  it should "leave empty strings" in {
    val input =
      """
        | {
        |   "name": "Roxie",
        |   "breed": "Rottie/lab cross",
        |   "nickname": "",
        |   "birth_year": null
        | }
      """.stripMargin

    val output = "{\"name\":\"Roxie\",\"breed\":\"Rottie/lab cross\",\"nickname\":\"\"}"

    JsonCleaner.clean(input) shouldBe output
  }

  it should "just return back the input if it's not a JsObject" in {

    val input = "Roxie"
    JsonCleaner.clean(input) shouldBe input
  }

  it should "still return back the input if it's not a JsObject even if it's a null" in {
    JsonCleaner.clean(null) shouldBe null
  }


}