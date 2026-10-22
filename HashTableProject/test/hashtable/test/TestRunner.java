package hashtable.test;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("      CSD201 TEST SUITE - NHÓM 1 (FA26)         ");
        System.out.println("=================================================\n");

        int failures = 0;
        failures += runSuite("HashTableOracleTest", HashTableOracleTest::runAllTests);
        failures += runSuite("C1SeparateChainingTest", C1SeparateChainingTest::runAllTests);
        failures += runSuite("C2HashTableTest", C2HashTableTest::runAllTests);
        failures += runSuite("LinearProbingTombstoneTest", LinearProbingTombstoneTest::runAllTests);
        failures += runSuite("RobinHoodAndBackshiftTest", RobinHoodAndBackshiftTest::runAllTests);
        failures += runSuite("LinearProbingBackshiftTest", LinearProbingBackshiftTest::runAllTests);
        failures += runSuite("RobinHoodTableTest", RobinHoodTableTest::runAllTests);
        failures += runSuite("VariantBStringTest", VariantBStringTest::runAllTests);
        failures += runSuite("C3HashTableTest", C3HashTableTest::runAllTests);
        failures += runSuite("BenchmarkRunnerTest", BenchmarkRunnerTest::runAllTests);

        if (failures == 0) {
            System.out.println(">>> TẤT CẢ CÁC BÀI TEST ĐÃ HOÀN THÀNH XUẤT SẮC! <<<");
        } else {
            System.err.println("!!! CÓ " + failures + " TEST SUITE THẤT BẠI !!!");
            System.exit(1);
        }
    }

    private static int runSuite(String name, Runnable suite) {
        try {
            suite.run();
            return 0;
        } catch (Throwable t) {
            System.err.println("!!! Test suite thất bại: " + name + " !!!");
            t.printStackTrace();
            return 1;
        }
    }
}
