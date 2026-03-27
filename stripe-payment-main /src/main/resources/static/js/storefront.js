(function () {
    const TOKEN_KEY = "jwt_token";
    const CART_KEY = "checkout_studio_cart";

    const PRODUCTS = [
        {
            name: "Smartphone",
            price: 15000,
            image: "/images/smartphone.jpg",
            pill: "Best Seller",
            description: "A sleek everyday device built for fast browsing, smooth streaming, and reliable battery life."
        },
        {
            name: "Headphones",
            price: 5000,
            image: "/images/headphones.jpg",
            pill: "Studio Pick",
            description: "Balanced sound, deep bass, and a comfortable over-ear fit for long listening sessions."
        },
        {
            name: "Laptop",
            price: 75000,
            image: "/images/laptop.jpg",
            pill: "Pro Choice",
            description: "A high-performance machine for coding, design work, and day-to-day multitasking."
        },
        {
            name: "Mirrorless Camera",
            price: 30000,
            image: "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=800&q=80",
            pill: "Creator Kit",
            description: "Capture sharp shots and cinematic clips with a travel-friendly mirrorless profile."
        },
        {
            name: "Smart Watch",
            price: 12000,
            image: "https://images.unsplash.com/photo-1544117519-31a4b719223d?auto=format&fit=crop&w=800&q=80",
            pill: "Fitness Sync",
            description: "Track workouts, messages, and daily health stats from a bright wrist-ready display."
        },
        {
            name: "Wireless Earbuds",
            price: 8000,
            image: "https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=800&q=80",
            pill: "Pocket Audio",
            description: "Compact true-wireless earbuds with noise isolation and a fast-charging carry case."
        },
        {
            name: "Bluetooth Speaker",
            price: 9500,
            image: "https://images.unsplash.com/photo-1589003077984-894e133dabab?auto=format&fit=crop&w=800&q=80",
            pill: "Room Audio",
            description: "A compact speaker with strong bass response, clean vocals, and portable battery life."
        },
        {
            name: "Gaming Console",
            price: 45000,
            image: "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?auto=format&fit=crop&w=800&q=80",
            pill: "Next Gen",
            description: "A living-room-ready console built for smooth graphics, fast loads, and immersive play."
        },
        {
            name: "Drone",
            price: 52000,
            image: "https://images.unsplash.com/photo-1473968512647-3e447244af8f?auto=format&fit=crop&w=800&q=80",
            pill: "Aerial Shot",
            description: "Fly stabilized 4K footage with responsive controls and intelligent return-to-home safety."
        }
    ];

    let currentOrders = [];
    let pendingCancelOrderId = null;

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function isLoggedIn() {
        return !!getToken();
    }

    function getCart() {
        try {
            return JSON.parse(localStorage.getItem(CART_KEY) || "[]");
        } catch (error) {
            return [];
        }
    }

    function saveCart(cart) {
        localStorage.setItem(CART_KEY, JSON.stringify(cart));
        updateNavCartCount();
    }

    function clearCart() {
        saveCart([]);
    }

    function formatPrice(cents) {
        return `$${(cents / 100).toFixed(2)}`;
    }

    function setFeedback(element, message, type) {
        if (!element) {
            return;
        }
        element.textContent = message || "";
        element.className = type ? `feedback ${type}` : "feedback";
    }

    function updateNavCartCount() {
        const count = getCart().reduce((sum, item) => sum + item.quantity, 0);
        document.querySelectorAll("[data-cart-count]").forEach(node => {
            node.textContent = String(count);
        });
        document.querySelectorAll("[data-auth-only]").forEach(node => {
            node.style.display = isLoggedIn() ? "inline-flex" : "none";
        });
        document.querySelectorAll("[data-guest-only]").forEach(node => {
            node.style.display = isLoggedIn() ? "none" : "inline-flex";
        });
    }

    function logoutUser(redirect) {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(CART_KEY);
        if (redirect) {
            window.location.href = redirect;
        }
    }

    async function requestJson(url, options) {
        const response = await fetch(url, options);
        const contentType = response.headers.get("content-type") || "";
        const body = contentType.includes("application/json") ? await response.json() : null;
        if (!response.ok) {
            const message = body && body.error ? body.error : `Request failed: ${response.status}`;
            throw new Error(message);
        }
        return body;
    }

    function renderProductGrid() {
        const container = document.getElementById("product-grid");
        if (!container) {
            return;
        }

        container.innerHTML = PRODUCTS.map(product => `
            <article class="product-card" data-product-card data-name="${product.name}" data-price="${product.price}">
                <div class="product-visual">
                    <span class="product-pill">${product.pill}</span>
                    <img src="${product.image}" alt="${product.name}">
                </div>
                <div class="product-body">
                    <h3 class="product-title">${product.name}</h3>
                    <p class="product-subtitle">${product.description}</p>
                    <p class="product-price">${formatPrice(product.price)}</p>
                    <div class="quantity-controls">
                        <button type="button" data-decrement>-</button>
                        <input type="text" class="form-control quantity-display" value="1" readonly>
                        <button type="button" data-increment>+</button>
                    </div>
                    <button class="btn btn-primary full-width" type="button" data-add-to-cart>Add to Cart</button>
                </div>
            </article>
        `).join("");

        container.querySelectorAll("[data-product-card]").forEach(card => {
            const quantityInput = card.querySelector(".quantity-display");
            const priceNode = card.querySelector(".product-price");
            const unitAmount = Number(card.dataset.price);

            card.querySelector("[data-decrement]").addEventListener("click", () => {
                const next = Math.max(1, Number(quantityInput.value) - 1);
                quantityInput.value = String(next);
                priceNode.textContent = formatPrice(unitAmount * next);
            });

            card.querySelector("[data-increment]").addEventListener("click", () => {
                const next = Number(quantityInput.value) + 1;
                quantityInput.value = String(next);
                priceNode.textContent = formatPrice(unitAmount * next);
            });

            card.querySelector("[data-add-to-cart]").addEventListener("click", () => {
                const cart = getCart();
                const quantity = Number(quantityInput.value);
                const existing = cart.find(item => item.productName === card.dataset.name && item.unitAmount === unitAmount);
                if (existing) {
                    existing.quantity += quantity;
                } else {
                    cart.push({
                        productName: card.dataset.name,
                        unitAmount,
                        quantity
                    });
                }
                saveCart(cart);
                const feedback = document.getElementById("products-feedback");
                setFeedback(feedback, `${card.dataset.name} added to cart.`, "success");
            });
        });
    }

    function renderHomeState() {
        const authCard = document.getElementById("auth-card");
        const welcomeCard = document.getElementById("welcome-card");
        if (!authCard || !welcomeCard) {
            return;
        }
        authCard.style.display = isLoggedIn() ? "none" : "block";
        welcomeCard.style.display = isLoggedIn() ? "block" : "none";
    }

    function bindAuthForms() {
        const authCard = document.getElementById("auth-card");
        if (!authCard) {
            return;
        }

        const toggleBtn = document.getElementById("toggle-auth");
        const loginBtn = document.getElementById("login-btn");
        const signupBtn = document.getElementById("signup-btn");
        const authTitle = document.getElementById("auth-title");
        const signupExtra = document.getElementById("signup-extra");
        const feedback = document.getElementById("auth-feedback");
        let signupMode = false;

        function setMode(signup) {
            signupMode = signup;
            setFeedback(feedback, "", "");
            authTitle.textContent = signup ? "Create Your Account" : "Login to Continue";
            loginBtn.style.display = signup ? "none" : "block";
            signupBtn.style.display = signup ? "block" : "none";
            signupExtra.style.display = signup ? "block" : "none";
            toggleBtn.textContent = signup ? "Already have an account? Login" : "Don't have an account? Sign Up";
        }

        toggleBtn.addEventListener("click", () => setMode(!signupMode));

        loginBtn.addEventListener("click", async () => {
            try {
                const body = await requestJson("/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        username: document.getElementById("username").value,
                        password: document.getElementById("password").value
                    })
                });
                localStorage.setItem(TOKEN_KEY, body.token);
                updateNavCartCount();
                renderHomeState();
                window.location.href = "/products";
            } catch (error) {
                setFeedback(feedback, error.message, "error");
            }
        });

        signupBtn.addEventListener("click", async () => {
            try {
                await requestJson("/auth/register", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        username: document.getElementById("username").value,
                        password: document.getElementById("password").value,
                        email: document.getElementById("email").value
                    })
                });
                document.getElementById("password").value = "";
                setMode(false);
                setFeedback(feedback, "Signup successful. Please login to continue.", "success");
            } catch (error) {
                setFeedback(feedback, error.message || "Signup failed. Try a different username or email.", "error");
            }
        });

        setMode(false);
    }

    function renderCartPage() {
        const itemsContainer = document.getElementById("cart-items");
        if (!itemsContainer) {
            return;
        }

        const cart = getCart();
        const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
        const totalAmount = cart.reduce((sum, item) => sum + item.unitAmount * item.quantity, 0);

        const countNode = document.getElementById("cart-item-count");
        const totalNode = document.getElementById("cart-total");
        const checkoutButton = document.getElementById("cart-checkout-btn");
        const feedback = document.getElementById("cart-feedback");

        if (countNode) {
            countNode.textContent = `${totalItems} item${totalItems === 1 ? "" : "s"}`;
        }
        if (totalNode) {
            totalNode.textContent = formatPrice(totalAmount);
        }

        if (cart.length === 0) {
            itemsContainer.innerHTML = `
                <section class="empty-state panel">
                    <h3>Your cart is empty</h3>
                    <p class="muted">Browse products, add a few favorites, and return here when you are ready to check out.</p>
                    <div class="cta-row">
                        <a class="btn btn-primary" href="/products">Browse Products</a>
                    </div>
                </section>
            `;
            if (checkoutButton) {
                checkoutButton.disabled = true;
            }
            return;
        }

        itemsContainer.innerHTML = cart.map((item, index) => `
            <article class="cart-item">
                <div>
                    <h4>${item.productName}</h4>
                    <p class="muted">${item.quantity} x ${formatPrice(item.unitAmount)}</p>
                    <p class="muted">Line total: ${formatPrice(item.unitAmount * item.quantity)}</p>
                </div>
                <button class="link-button" type="button" data-remove-index="${index}">Remove</button>
            </article>
        `).join("");

        itemsContainer.querySelectorAll("[data-remove-index]").forEach(button => {
            button.addEventListener("click", () => {
                const nextCart = getCart();
                nextCart.splice(Number(button.dataset.removeIndex), 1);
                saveCart(nextCart);
                renderCartPage();
            });
        });

        if (checkoutButton) {
            checkoutButton.disabled = false;
            checkoutButton.onclick = async () => {
                if (!isLoggedIn()) {
                    setFeedback(feedback, "Please login first. You will be taken back to the home page.", "error");
                    setTimeout(() => {
                        window.location.href = "/?auth=login";
                    }, 1200);
                    return;
                }

                try {
                    setFeedback(feedback, "Preparing your secure checkout session...", "info");
                    const payment = await createStripeCheckout(totalItems, totalAmount);
                    clearCart();
                    renderCartPage();
                    if (window.Stripe) {
                        const stripe = Stripe("pk_test_51TDSBz9CYjE1QVn0yKISZv9Kito0xQIQTIT9Xrnm2aYxmi6otRQ7rwqvNT8Izzkr6b2fW9ABhBNZBAcGuYJt05pB00CX6LR2tJ");
                        const result = await stripe.redirectToCheckout({ sessionId: payment.sessionId });
                        if (result.error) {
                            throw new Error(result.error.message);
                        }
                    }
                } catch (error) {
                    setFeedback(feedback, error.message, "error");
                }
            };
        }
    }

    async function createStripeCheckout(totalItems, totalAmount) {
        const jwt = getToken();
        const cart = getCart();
        const productName = cart.length === 1 ? cart[0].productName : `Cart Order (${totalItems} items)`;

        const order = await requestJson("/api/orders", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${jwt}`
            },
            body: JSON.stringify({
                productName,
                unitAmount: totalAmount,
                quantity: 1,
                currency: "USD"
            })
        });

        return requestJson("/api/payments", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${jwt}`
            },
            body: JSON.stringify({
                orderId: order.id,
                provider: "STRIPE",
                idempotencyKey: crypto.randomUUID()
            })
        });
    }

    function getChipClass(value) {
        if (value === "SUCCEEDED" || value === "CONFIRMED" || value === "REFUNDED") {
            return "success";
        }
        if (value === "CANCELLED" || value === "PAYMENT_FAILED") {
            return "cancelled";
        }
        return "";
    }

    function renderOrders() {
        const ordersList = document.getElementById("orders-list");
        if (!ordersList) {
            return;
        }
        if (!currentOrders.length) {
            ordersList.innerHTML = '<section class="empty-state panel"><h3>No orders yet</h3><p class="muted">Complete your first checkout and this page will turn into your order command center.</p></section>';
            return;
        }

        ordersList.innerHTML = currentOrders.map(order => {
            const canCancel = order.orderStatus === "CONFIRMED" && order.paymentStatus === "SUCCEEDED";
            const awaitingConfirm = pendingCancelOrderId === order.id;
            return `
                <article class="order-card">
                    <h4>${order.productName}</h4>
                    <p class="order-meta">Order #${order.id}</p>
                    <p class="order-meta">Quantity: ${order.quantity}</p>
                    <p class="order-meta">Total: ${formatPrice(order.totalAmount)}</p>
                    <div class="order-chip-row">
                        <span class="order-chip ${getChipClass(order.orderStatus)}">${order.orderStatus}</span>
                        <span class="order-chip ${getChipClass(order.paymentStatus)}">${order.paymentStatus}</span>
                    </div>
                    ${order.orderStatus === "CANCELLED" ? '<p class="order-note">This order has been cancelled and the refund has already been initiated.</p>' : ""}
                    <div class="order-actions">
                        ${canCancel && !awaitingConfirm ? `<button class="btn btn-danger" type="button" data-cancel-order="${order.id}">Cancel Order</button>` : ""}
                        ${awaitingConfirm ? `
                            <button class="btn btn-danger" type="button" data-confirm-cancel="${order.id}">Confirm Cancel</button>
                            <button class="btn btn-secondary" type="button" data-keep-order="${order.id}">Keep Order</button>
                        ` : ""}
                    </div>
                </article>
            `;
        }).join("");

        const feedback = document.getElementById("orders-feedback");
        ordersList.querySelectorAll("[data-cancel-order]").forEach(button => {
            button.addEventListener("click", () => {
                pendingCancelOrderId = Number(button.dataset.cancelOrder);
                setFeedback(feedback, `Confirm cancellation for order #${pendingCancelOrderId}. A refund will be requested immediately.`, "info");
                renderOrders();
            });
        });

        ordersList.querySelectorAll("[data-keep-order]").forEach(button => {
            button.addEventListener("click", () => {
                pendingCancelOrderId = null;
                setFeedback(feedback, "", "");
                renderOrders();
            });
        });

        ordersList.querySelectorAll("[data-confirm-cancel]").forEach(button => {
            button.addEventListener("click", async () => {
                try {
                    await requestJson(`/api/orders/${button.dataset.confirmCancel}/cancel`, {
                        method: "POST",
                        headers: {
                            "Authorization": `Bearer ${getToken()}`
                        }
                    });
                    pendingCancelOrderId = null;
                    await loadOrders();
                    setFeedback(feedback, `Order #${button.dataset.confirmCancel} was cancelled successfully and the refund has been initiated.`, "success");
                } catch (error) {
                    setFeedback(feedback, error.message, "error");
                }
            });
        });
    }

    async function loadOrders() {
        if (!document.getElementById("orders-list")) {
            return;
        }
        const feedback = document.getElementById("orders-feedback");
        if (!isLoggedIn()) {
            currentOrders = [];
            pendingCancelOrderId = null;
            setFeedback(feedback, "Login to view your recent orders and refunds.", "info");
            renderOrders();
            return;
        }

        try {
            currentOrders = await requestJson("/api/orders", {
                headers: {
                    "Authorization": `Bearer ${getToken()}`
                }
            });
            renderOrders();
        } catch (error) {
            currentOrders = [];
            setFeedback(feedback, error.message, "error");
            renderOrders();
        }
    }

    async function loadProfile() {
        const profileRoot = document.getElementById("profile-root");
        if (!profileRoot) {
            return;
        }

        if (!isLoggedIn()) {
            profileRoot.innerHTML = `
                <section class="empty-state panel">
                    <h3>Profile locked</h3>
                    <p class="muted">Login first to view your account details, order status, and refund actions.</p>
                    <div class="cta-row">
                        <a class="btn btn-primary" href="/">Go To Login</a>
                    </div>
                </section>
            `;
            return;
        }

        try {
            const profile = await requestJson("/api/users/me", {
                headers: {
                    "Authorization": `Bearer ${getToken()}`
                }
            });
            profileRoot.innerHTML = `
                <section class="panel">
                    <div class="section-head">
                        <div>
                            <h3>${profile.username}</h3>
                            <p class="section-subtitle">Signed in and ready to manage orders.</p>
                        </div>
                    </div>
                    <div class="profile-grid">
                        <article class="info-card">
                            <strong>Email</strong>
                            <p>${profile.email}</p>
                        </article>
                        <article class="info-card">
                            <strong>User ID</strong>
                            <p>${profile.id}</p>
                        </article>
                    </div>
                </section>
            `;
        } catch (error) {
            profileRoot.innerHTML = `
                <section class="empty-state panel">
                    <h3>Profile unavailable</h3>
                    <p class="muted">${error.message}</p>
                </section>
            `;
        }
    }

    async function loadEventAudit() {
        const eventsList = document.getElementById("events-list");
        const feedback = document.getElementById("events-feedback");
        if (!eventsList) {
            return;
        }

        if (!isLoggedIn()) {
            eventsList.innerHTML = `
                <section class="empty-state panel">
                    <h3>Event audit locked</h3>
                    <p class="muted">Login to inspect Kafka-consumed order and payment events.</p>
                </section>
            `;
            setFeedback(feedback, "Login to view recent event activity.", "info");
            return;
        }

        try {
            const events = await requestJson("/api/events/recent", {
                headers: {
                    "Authorization": `Bearer ${getToken()}`
                }
            });
            if (!events || !events.length) {
                eventsList.innerHTML = `
                    <section class="empty-state panel">
                        <h3>No events yet</h3>
                        <p class="muted">Start an order, payment, or refund flow and recent Kafka-consumed events will appear here.</p>
                    </section>
                `;
                setFeedback(feedback, "", "");
                return;
            }

            eventsList.innerHTML = events.map(event => `
                <article class="event-item">
                    <code>${event}</code>
                </article>
            `).join("");
            setFeedback(feedback, "Latest event stream loaded successfully.", "success");
        } catch (error) {
            eventsList.innerHTML = `
                <section class="empty-state panel">
                    <h3>Event audit unavailable</h3>
                    <p class="muted">${error.message}</p>
                </section>
            `;
            setFeedback(feedback, error.message, "error");
        }
    }

    async function confirmPaymentFromReturn() {
        const sessionId = new URLSearchParams(window.location.search).get("session_id");
        if (!sessionId || !isLoggedIn()) {
            return;
        }
        try {
            await requestJson(`/api/payments/confirm/${sessionId}`, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${getToken()}`
                }
            });
        } catch (error) {
            console.error("Unable to confirm payment from return flow:", error);
        } finally {
            const cleanUrl = `${window.location.pathname}${window.location.hash || ""}`;
            window.history.replaceState({}, "", cleanUrl);
        }
    }

    function bindLogoutPage() {
        const logoutCard = document.getElementById("logout-card");
        if (!logoutCard) {
            return;
        }
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(CART_KEY);
        updateNavCartCount();
        const link = document.getElementById("logout-home-link");
        if (link) {
            link.focus();
        }
    }

    function bindGlobalActions() {
        document.querySelectorAll("[data-logout-link]").forEach(node => {
            node.addEventListener("click", event => {
                event.preventDefault();
                window.location.href = "/logout-page";
            });
        });
    }

    document.addEventListener("DOMContentLoaded", async () => {
        updateNavCartCount();
        bindGlobalActions();
        await confirmPaymentFromReturn();
        bindAuthForms();
        renderHomeState();
        renderProductGrid();
        renderCartPage();
        await loadProfile();
        await loadOrders();
        await loadEventAudit();
        bindLogoutPage();
    });
})();
