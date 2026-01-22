import { getMessaging } from 'firebase-admin/messaging'
import { getFirestore } from 'firebase-admin/firestore'

export async function sendExpenseNotification(
    userIds: string[],
    title: string,
    body: string,
    category: string
) {
    const db = getFirestore()

    const users = await Promise.all(
        userIds.map(async (userId) => {
            const user = await db.collection('users').doc(userId).get()
            return user.data()
        })
    )

    const tokens = users.map(
        (user) => user?.fcmToken
    ).filter(
        (token) => token !== undefined
    )

    if (tokens.length === 0) return

    await getMessaging().sendEachForMulticast({
        tokens,
        data: {
            title,
            body,
            category,
            type: "GROUP_EXPENSE_ADDED"
        },
    });
}
