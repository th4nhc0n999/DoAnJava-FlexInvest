package Controller;

import DAO.InvestmentDAO;
import Model.SavingsProduct;

public class SavingsProductController {
    
    private final InvestmentDAO investmentDAO;

    public SavingsProductController() {
        this.investmentDAO = new InvestmentDAO();
    }

    public boolean createProduct(SavingsProduct p) {
        if (p.getMinInvestmentAmount().compareTo(p.getMaxInvestmentAmount()) >= 0 && p.getMaxInvestmentAmount().signum() > 0) {
            System.err.println("Min investment must be less than Max investment.");
            return false;
        }
        if (p.getInterestRate().signum() <= 0) {
            System.err.println("Interest rate must be greater than 0.");
            return false;
        }
        if (p.getTerm() < 0) {
            System.err.println("Term cannot be negative.");
            return false;
        }
        
        int newId = investmentDAO.insertProduct(p);
        return newId > 0;
    }

    public boolean updateProduct(SavingsProduct p) {
        if (investmentDAO.hasActiveInvestments(p.getProductId())) {
            System.err.println("Cannot update product: There are ACTIVE investments using this product.");
            return false;
        }
        return investmentDAO.updateProduct(p);
    }

    public boolean toggleActive(int productId, String currentStatus) {
        String newStatus = "ACTIVE".equals(currentStatus) ? "INACTIVE" : "ACTIVE";
        return investmentDAO.toggleProductStatus(productId, newStatus);
    }
}
