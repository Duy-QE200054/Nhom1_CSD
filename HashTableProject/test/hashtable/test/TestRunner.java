package hashtable.test;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("      CSD201 TEST SUITE - NHÓM 1 (FA26)         ");
        System.out.println("=================================================\n");

        try {
            C1SeparateChainingTest.runAllTests();
            LinearProbingTombstoneTest.runAllTests();
            RobinHoodAndBackshiftTest.runAllTests();
            LinearProbingBackshiftTest.runAllTests();
            RobinHoodTableTest.runAllTests();
            VariantBStringTest.runAllTests();
            System.out.println(">>> TẤT CẢ CÁC BÀI TEST ĐÃ HOÀN THÀNH XUẤT SẮC! <<<");
        } catch (Throwable t) {
            System.err.println("!!! CÓ LỖI XẢY RA TRONG QUÁ TRÌNH CHẠY TEST !!!");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
