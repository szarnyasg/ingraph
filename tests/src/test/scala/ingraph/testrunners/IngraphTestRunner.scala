package ingraph.testrunners

import java.util.concurrent.TimeUnit

import com.google.common.base.Stopwatch
import ingraph.driver.CypherDriverFactory
import ingraph.ire.IngraphOneTimeAdapter
import ingraph.tests.LdbcSnbTestCase
import org.supercsv.prefs.CsvPreference

import scala.collection.JavaConverters._

object IngraphTestRunner {

  def run(tc: LdbcSnbTestCase) : List[Map[String, Any]] = {
    val driver = CypherDriverFactory.createIngraphDriver
    try {
      val session = driver.session
      val csvPreference = new CsvPreference.Builder('"', '|', "\n").build
      val queryHandler = session.registerQuery(tc.name, tc.query)
      queryHandler.readCsv(
        tc.vertexCsvPaths,
        tc.edgeCsvPaths,
        csvPreference
      )
      val res1 = queryHandler.result

      val indexer = queryHandler.adapter.indexer

      //      val loader = new LdbcUpdateToIngraphLoader(indexer, "../graphs/ldbc-snb-bi/sf-tiny/")
      //      loader.load()

      val s = Stopwatch.createStarted()
      val onetime = new IngraphOneTimeAdapter(
        // sftiny: 8796093022246
        // sf01: 32985348834423
        // sf03: 13194139533500
        // sf1: 4398046516185
        """
          |MATCH (p:Person {id: 32985348834423})
          |DETACH DELETE p
        """.stripMargin,
        "del", indexer)
      onetime.terminate()
      val res2 = queryHandler.result

      println("Update time: " + s.elapsed(TimeUnit.MILLISECONDS))

      res2
    } finally if (driver != null) {
      driver.close()
    }
  }

}
