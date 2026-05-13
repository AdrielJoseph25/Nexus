const API_BASE = localStorage.getItem("nexusApiBase") || "http://localhost:8080/api";

const state = {
    orders: [],
    inventory: [],
    payments: []
};

const $ = (selector) => document.querySelector(selector);

function shortId(value) {
    if (!value) return "-";
    return `${value}`.slice(0, 8);
}

function money(value) {
    if (value === null || value === undefined) return "-";
    return Number(value).toLocaleString(undefined, { style: "currency", currency: "USD" });
}

function badge(status) {
    const normalized = `${status || "unknown"}`.toLowerCase();
    return `<span class="badge ${normalized}">${status || "UNKNOWN"}</span>`;
}

async function fetchJson(path) {
    const response = await fetch(`${API_BASE}${path}`);
    if (!response.ok) {
        throw new Error(`${path} returned ${response.status}`);
    }
    return response.json();
}

async function createOrder(payload) {
    const response = await fetch(`${API_BASE}/orders`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    });
    if (!response.ok) {
        throw new Error(`Order request failed with ${response.status}`);
    }
    return response.json();
}

function renderOrders() {
    $("#ordersBody").innerHTML = state.orders.map((order) => `
        <tr>
            <td><code>${shortId(order.id)}</code></td>
            <td>${order.customerId || "-"}</td>
            <td>${order.sku || "-"}</td>
            <td>${order.quantity ?? "-"}</td>
            <td>${badge(order.status)}</td>
        </tr>
    `).join("") || `<tr><td colspan="5">No orders yet.</td></tr>`;
}

function renderInventory() {
    $("#inventoryBody").innerHTML = state.inventory.map((item) => `
        <tr>
            <td>${item.sku || "-"}</td>
            <td>${item.availableQuantity ?? 0}</td>
            <td>${item.reservedQuantity ?? 0}</td>
        </tr>
    `).join("") || `<tr><td colspan="3">Inventory service has not returned data.</td></tr>`;
}

function renderPayments() {
    $("#paymentsBody").innerHTML = state.payments.map((payment) => `
        <tr>
            <td><code>${shortId(payment.id)}</code></td>
            <td><code>${shortId(payment.orderId)}</code></td>
            <td>${money(payment.amount)}</td>
            <td>${badge(payment.status)}</td>
        </tr>
    `).join("") || `<tr><td colspan="4">No payments yet.</td></tr>`;
}

function renderStats() {
    const confirmed = state.orders.filter((order) => order.status === "CONFIRMED").length;
    const reserved = state.inventory.reduce((sum, item) => sum + Number(item.reservedQuantity || 0), 0);

    $("#orderCount").textContent = state.orders.length;
    $("#confirmedCount").textContent = confirmed;
    $("#reservedCount").textContent = reserved;
    $("#paymentCount").textContent = state.payments.length;
}

function renderAll() {
    renderStats();
    renderOrders();
    renderInventory();
    renderPayments();
}

async function refresh() {
    $("#gatewayStatus").textContent = "Checking";
    try {
        const [orders, inventory, payments] = await Promise.all([
            fetchJson("/orders"),
            fetchJson("/inventory"),
            fetchJson("/payments")
        ]);
        state.orders = orders;
        state.inventory = inventory;
        state.payments = payments;
        $("#gatewayStatus").textContent = "Online";
        renderAll();
    } catch (error) {
        $("#gatewayStatus").textContent = "Offline";
        $("#formMessage").textContent = `Could not reach Gateway at ${API_BASE}. Start backend services first.`;
    }
}

$("#orderForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = {
        customerId: form.get("customerId"),
        sku: form.get("sku"),
        quantity: Number(form.get("quantity")),
        amount: Number(form.get("amount"))
    };

    $("#formMessage").textContent = "Creating order and starting saga...";
    try {
        const order = await createOrder(payload);
        $("#formMessage").textContent = `Order ${shortId(order.id)} accepted. Refreshing saga state...`;
        setTimeout(refresh, 900);
    } catch (error) {
        $("#formMessage").textContent = error.message;
    }
});

$("#rollbackDemo").addEventListener("click", () => {
    $("#orderForm").querySelector('[name="amount"]').value = "7500.00";
    $("#formMessage").textContent = "Amount set above payment limit to trigger rollback.";
});

$("#refreshButton").addEventListener("click", refresh);

refresh();
