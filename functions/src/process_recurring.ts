import { onSchedule } from 'firebase-functions/scheduler';
import { getFirestore } from 'firebase-admin/firestore';
import { v4 as uuidv4 } from 'uuid';

enum RecurrenceType {
    DAILY = 'DAILY',
    WEEKLY = 'WEEKLY',
    MONTHLY = 'MONTHLY',
    YEARLY = 'YEARLY',
}

const calculateNextOccurrence = (
    from: number,
    after: RecurrenceType
): number => {
    const date = new Date(from);
    date.setUTCHours(0, 0, 0, 0);

    switch (after) {
        case RecurrenceType.DAILY:
            date.setUTCDate(date.getUTCDate() + 1);
            break;

        case RecurrenceType.WEEKLY:
            date.setUTCDate(date.getUTCDate() + 7);
            break;

        case RecurrenceType.MONTHLY:
            date.setUTCMonth(date.getUTCMonth() + 1);
            break;

        case RecurrenceType.YEARLY:
            date.setUTCFullYear(date.getUTCFullYear() + 1);
            break;
    }

    return date.getTime();
}

export const processRecurring = onSchedule(
    "every day 00:00",
    async () => {
        const db = getFirestore()
        const now = Date.now()

        const templates = await db
            .collectionGroup('recurring_expenses')
            .where('paused', '==', false)
            .where('nextOccurrenceAt', '<=', now)
            .get();

        for (const doc of templates.docs) {
            const template = doc.data()
            const templateRef = doc.ref

            if (template.endAt && template.nextOccurrenceAt > template.endAt) {
                await templateRef.delete()
                continue
            }

            const path = templateRef.path.split('/')
            const ownerCollection = path[0]
            const ownerId = path[1]

            const expenseId = uuidv4()

            const expense = {
                ...template.expenseData,
                id: expenseId,
                timestamp: template.nextOccurrenceAt,
                createdAt: now,
            }

            await db
                .collection(ownerCollection)
                .doc(ownerId)
                .collection('expenses')
                .doc(expenseId)
                .set(expense)

            const nextOccurrence = calculateNextOccurrence(
                template.nextOccurrenceAt,
                template.frequency as RecurrenceType
            )

            await templateRef.update({
                nextOccurrenceAt: nextOccurrence,
            })
        }
    }
)
