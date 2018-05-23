package bn;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import bn.Controller;

class ResultsOrganizer {

  private static final String RESULTS_DIR = "./results";

  public static void main(String args[]) throws Exception {
    String pattern = args[0];
    if (args.length > 1) Controller.setUrl(args[1]);
    File directory = new File(RESULTS_DIR);
    if (! directory.exists()) directory.mkdirs();

    Controller controller = Controller.getInstance();
    String[] availableResults = controller.getResultsKeys(pattern);
    for (String key : availableResults) {
      String header = controller.getResultsHeader(key);
      String[] results = controller.getResults(key);
      String fileName = RESULTS_DIR + "/" + key + ".csv";
      PrintWriter output = new PrintWriter(new FileWriter(fileName, false));
      output.println(header);
      for (String resLine : results) {
        output.println(resLine);
      }
      output.close();
    }
  }
}
