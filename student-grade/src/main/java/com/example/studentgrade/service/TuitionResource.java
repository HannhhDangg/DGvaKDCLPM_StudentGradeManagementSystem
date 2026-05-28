package com.example.studentgrade.service;

import com.example.studentgrade.model.*;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

@Path("/tuition")
@Produces(MediaType.TEXT_HTML)
public class TuitionResource {

    @Inject
    EntityManager em;

    @Inject
    @Location("tuition")
    Template tuitionTemplate;

    @GET
    public String getTuitionPage(@QueryParam("semesterId") String semesterIdStr,
            @QueryParam("status") String status,
            @QueryParam("search") String search,
            @QueryParam("page") @DefaultValue("1") int page) {

        Long semesterId = null;
        if (semesterIdStr != null && !semesterIdStr.trim().isEmpty()) {
            try {
                semesterId = Long.parseLong(semesterIdStr);
            } catch (NumberFormatException e) {
            }
        }
        List<Semester> semesters = em.createQuery("SELECT s FROM Semester s ORDER BY s.startDate DESC", Semester.class)
                .getResultList();

        int pageSize = 10;

        String qlString = "SELECT t FROM TuitionFee t WHERE 1=1";
        Map<String, Object> params = new HashMap<>();

        if (semesterId != null && semesterId > 0) {
            qlString += " AND t.semesterId = :semId";
            params.put("semId", semesterId);
        }

        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            qlString += " AND t.status = :status";
            params.put("status", status);
        }

        if (search != null && !search.trim().isEmpty()) {
            qlString += " AND t.studentId IN (SELECT s.id FROM Student s WHERE lower(s.lastName) LIKE :search OR lower(s.firstName) LIKE :search OR lower(s.username) LIKE :search)";
            params.put("search", "%" + search.toLowerCase() + "%");
        }

        qlString += " ORDER BY t.id DESC";

        var query = em.createQuery(qlString, TuitionFee.class);
        params.forEach(query::setParameter);

        List<TuitionFee> allFees = query.getResultList();
        int totalRecords = allFees.size();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        List<TuitionFee> pagedFees = query.setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();

        List<Map<String, Object>> feeList = new ArrayList<>();
        double totalDebt = 0.0;
        for (TuitionFee f : allFees) {
            totalDebt += (f.getTotalAmount() - f.getPaidAmount());
        }

        java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        for (TuitionFee f : pagedFees) {
            Map<String, Object> map = new HashMap<>();
            Student student = em.find(Student.class, f.getStudentId());
            Semester sem = em.find(Semester.class, f.getSemesterId());

            map.put("id", f.getId());
            map.put("studentName", student != null ? student.getFullName() : "Không rõ");
            map.put("studentUsername", student != null ? student.getUsername() : "");
            map.put("semesterName", sem != null ? sem.getName() : "Không rõ");
            map.put("totalCredits", f.getTotalCredits());
            map.put("totalAmountStr", format.format(f.getTotalAmount()));
            map.put("paidAmountStr", format.format(f.getPaidAmount()));
            map.put("remainingAmountStr", format.format(f.getTotalAmount() - f.getPaidAmount()));
            map.put("status", f.getStatus());

            List<PaymentTransaction> txs = em
                    .createQuery("SELECT p FROM PaymentTransaction p WHERE p.tuitionFeeId = :tid ORDER BY p.id DESC",
                            PaymentTransaction.class)
                    .setParameter("tid", f.getId())
                    .getResultList();
            map.put("transactions", txs);

            feeList.add(map);
        }

        // Thống kê học phí toàn trường
        Double totalUncollectedTuitionDouble = em.createQuery("SELECT SUM(t.totalAmount - t.paidAmount) FROM TuitionFee t WHERE t.status = 'UNPAID'", Double.class).getSingleResult();
        double totalUncollectedTuition = totalUncollectedTuitionDouble != null ? totalUncollectedTuitionDouble : 0.0;
        
        Double totalCollectedTuitionDouble = em.createQuery("SELECT SUM(t.paidAmount) FROM TuitionFee t", Double.class).getSingleResult();
        double totalCollectedTuition = totalCollectedTuitionDouble != null ? totalCollectedTuitionDouble : 0.0;
        
        Long studentsUnpaidTuition = em.createQuery("SELECT COUNT(DISTINCT t.studentId) FROM TuitionFee t WHERE t.status = 'UNPAID'", Long.class).getSingleResult();
        Long totalStudentsWithTuition = em.createQuery("SELECT COUNT(DISTINCT t.studentId) FROM TuitionFee t", Long.class).getSingleResult();
        Long studentsPaidTuition = totalStudentsWithTuition - studentsUnpaidTuition;

        return tuitionTemplate.data("feeList", feeList)
                .data("semesters", semesters)
                .data("selectedSemesterId", semesterId)
                .data("selectedStatus", status == null ? "ALL" : status)
                .data("search", search == null ? "" : search)
                .data("currentPage", page)
                .data("totalPages", totalPages)
                .data("totalDebtStr", format.format(totalDebt))
                .data("totalUncollectedTuition", format.format(totalUncollectedTuition))
                .data("totalCollectedTuition", format.format(totalCollectedTuition))
                .data("totalUncollectedTuitionRaw", totalUncollectedTuition)
                .data("totalCollectedTuitionRaw", totalCollectedTuition)
                .data("studentsPaidTuition", studentsPaidTuition)
                .data("studentsUnpaidTuition", studentsUnpaidTuition)
                .render();
    }

    @POST
    @Path("/pay")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response recordPayment(@FormParam("tuitionFeeId") Long tuitionFeeId,
            @FormParam("amount") Double amount,
            @FormParam("method") String method,
            @FormParam("semesterId") Long returnSemesterId) {

        TuitionFee fee = em.find(TuitionFee.class, tuitionFeeId);
        if (fee != null && amount != null && amount > 0) {
            double currentPaid = fee.getPaidAmount() != null ? fee.getPaidAmount() : 0.0;
            double newPaid = currentPaid + amount;

            if (newPaid >= fee.getTotalAmount()) {
                fee.setPaidAmount(fee.getTotalAmount());
                fee.setStatus("PAID");
            } else {
                fee.setPaidAmount(newPaid);
                fee.setStatus("PARTIAL");
            }
            em.merge(fee);

            PaymentTransaction tx = new PaymentTransaction();
            tx.setTuitionFeeId(fee.getId());
            tx.setAmount(amount);
            tx.setPaymentDate(java.time.LocalDate.now().toString());
            tx.setMethod(method != null ? method : "Thủ công");
            em.persist(tx);
        }

        String redirect = "/tuition";
        if (returnSemesterId != null) {
            redirect += "?semesterId=" + returnSemesterId;
        }

        return Response.seeOther(URI.create(redirect)).build();
    }
}