import { initializeApp } from "firebase-admin/app";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { sendExpenseNotification } from "./notification";

initializeApp();

function formatCurrency(amount: number): string {
    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(amount);
}

export const onExpenseCreated = onDocumentCreated(
    "groups/{groupId}/expenses/{expenseId}",
    async (event) => {
        const expense = event.data?.data();
        if (!expense) return;

        const involvedUserIds = new Set<string>();

        expense.paidBy.forEach((p: any) => involvedUserIds.add(p.id));
        expense.splits.forEach((s: any) => involvedUserIds.add(s.id));

        const paidBy = expense.paidBy || [];
        const payerName = paidBy.length > 1
            ? "Multiple people"
            : paidBy.length === 1
                ? paidBy[0]?.name || "Someone"
                : "Someone";

        await sendExpenseNotification(
            Array.from(involvedUserIds),
            "You are involved in a new expense",
            `${payerName} paid ${formatCurrency(expense.amount)} for ${expense.details}`,
            expense.category
        );
    }
);
