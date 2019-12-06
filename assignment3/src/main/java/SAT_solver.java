import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.Stream;


public class SAT_solver {

    public static void main(String[] args) throws Exception {
        ISolver solver = SolverFactory.newDefault();
        Reader dimacsReader = new DimacsReader(solver);
        StringBuilder deadFeaturesStringBuilder = new StringBuilder();

        List<String> deadFeatureNrs = new ArrayList<>();

        String fileName = args[0];

        int nrOfImplications = 0;


        final HashMap<String, String> featureMap = new HashMap<>();

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            // parsing of file. extract featureNr and featureName
            stream
                    .filter(line -> line.charAt(0) == 'c')
                    .map(featureLine -> featureLine.split(" "))
                    .forEach(featureArr -> featureMap.put(featureArr[1], featureArr[2]));

        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            IProblem problem = dimacsReader.parseInstance(fileName);
            if (problem.isSatisfiable()) {
                // Dead features start //
                for (String feature_A : featureMap.keySet()) {
                    VecInt assumption = new VecInt(1, Integer.valueOf(feature_A));
                    if (isDead(assumption, problem)) {
                        deadFeaturesStringBuilder.append("Dead feature: #" + feature_A + ", Name: " + featureMap.get(feature_A) + "\n");
                        deadFeatureNrs.add(feature_A);
                    }
                }
                // Dead features end //


                // Print dead features
                System.out.println("Executing File : " + fileName);
                System.out.println("Dead Features:");
                System.out.println("====================================================");
                System.out.print(deadFeaturesStringBuilder.toString());
                System.out.println("====================================================");


                // Implications start //

                BufferedWriter writer = new BufferedWriter(new FileWriter("implications.txt"));


                for (String feature_A : featureMap.keySet()) {
                    for (String feature_B : featureMap.keySet()) {
                        int a_value = Integer.valueOf(feature_A);
                        int b_value = Integer.valueOf(feature_B);
                        // a -> b === -a V b
                        if (a_value < b_value) {
                            VecInt A_implies_B = new VecInt(1, -a_value);
                            A_implies_B.insertFirst(b_value);

                            VecInt B_implies_A = new VecInt(1, -b_value);
                            B_implies_A.insertFirst(a_value);


                            if (deadFeatureNrs.contains(feature_A) || deadFeatureNrs.contains(feature_B)) {
                                continue;
                            }

                            if (isDead(A_implies_B, problem)) {
                                nrOfImplications++;
                                writer.write(b_value + " -> " + a_value + "\n");
                            }
                            if (isDead(B_implies_A, problem)) {
                                nrOfImplications++;
                                writer.write(a_value + " -> " + b_value + "\n");
                            }
                        }
                    }
                }
                // Implications end //
                writer.close();
            }

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
        } catch (ContradictionException e) {
            System.out.println("Unsatisfiable (trivial)!");
        }

        // Print implications
        System.out.println("Number of implications: " + nrOfImplications);
    }


    private static boolean isDead(VecInt assumptions, IProblem problem) throws Exception {
        return !problem.isSatisfiable(assumptions);
    }


}
