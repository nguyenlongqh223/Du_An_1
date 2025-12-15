// Admin Account Information
const ADMIN_ACCOUNTS = [
    {
        username: 'admin',
        password: 'admin123',
        name: 'Administrator',
        role: 'super_admin'
    }
];

// Check if user is logged in
function checkAuth() {
    const isLoggedIn = sessionStorage.getItem('adminLoggedIn');
    if (!isLoggedIn && !window.location.pathname.includes('index.html')) {
        window.location.href = 'index.html';
    }
}

// Login function
function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');
    
    // Find admin account
    const admin = ADMIN_ACCOUNTS.find(acc => 
        acc.username === username && acc.password === password
    );
    
    if (admin) {
        // Store login info
        sessionStorage.setItem('adminLoggedIn', 'true');
        sessionStorage.setItem('adminUsername', admin.username);
        sessionStorage.setItem('adminName', admin.name);
        sessionStorage.setItem('adminRole', admin.role);
        
        // Redirect to dashboard
        window.location.href = 'dashboard.html';
    } else {
        errorMessage.textContent = 'Tên đăng nhập hoặc mật khẩu không đúng!';
        errorMessage.style.display = 'block';
    }
}

// Logout function
function logout() {
    sessionStorage.clear();
    window.location.href = 'index.html';
}

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    // Check if on login page
    if (document.getElementById('loginForm')) {
        document.getElementById('loginForm').addEventListener('submit', handleLogin);
    }
    
    // Check authentication on other pages
    if (!window.location.pathname.includes('index.html')) {
        checkAuth();
        
        // Display admin info in header if exists
        const adminName = sessionStorage.getItem('adminName');
        if (adminName) {
            const adminInfoElements = document.querySelectorAll('.admin-info');
            adminInfoElements.forEach(el => {
                el.textContent = `Xin chào, ${adminName}`;
            });
        }
    }
});

// API Configuration
const API_CONFIG = {
    BASE_URL: 'http://localhost:3000/api',
    // Hoặc nếu chạy trên mạng local, thay bằng IP của máy chạy server:
    // BASE_URL: 'http://192.168.0.100:3000/api',
};

// Helper function để gọi API
async function apiCall(endpoint, options = {}) {
    const url = `${API_CONFIG.BASE_URL}${endpoint}`;
    
    console.log(`API Call: ${options.method || 'GET'} ${url}`);
    
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
        },
    };
    
    const config = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers,
        },
    };
    
    // Convert body to JSON if it's an object
    if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
        config.body = JSON.stringify(config.body);
    }
    
    try {
        const response = await fetch(url, config);
        
        console.log(`API Response Status: ${response.status} ${response.statusText}`);
        
        // Handle empty response
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            if (response.ok) {
                return { success: true };
            } else {
                const errorText = await response.text();
                console.error('API Error (non-JSON):', errorText);
                throw new Error(`HTTP error! status: ${response.status} - ${errorText}`);
            }
        }
        
        const data = await response.json();
        console.log(`API Response Data:`, data);
        
        if (!response.ok) {
            throw new Error(data.message || data.error || `HTTP error! status: ${response.status}`);
        }
        
        return data;
    } catch (error) {
        console.error('API Error Details:', {
            endpoint,
            url,
            error: error.message,
            stack: error.stack
        });
        throw error;
    }
}

// Load data from API
async function loadData(type) {
    try {
        switch(type) {
            case 'customers':
                const usersResponse = await apiCall('/user');
                // Handle both wrapped and array formats
                let usersRaw = [];
                if (Array.isArray(usersResponse)) {
                    usersRaw = usersResponse;
                } else if (usersResponse && Array.isArray(usersResponse.data)) {
                    usersRaw = usersResponse.data;
                } else if (usersResponse && Array.isArray(usersResponse.users)) {
                    usersRaw = usersResponse.users;
                }

                // Transform users data to match expected format
                return usersRaw.map(user => ({
                    id: user._id || user.id,
                    _id: user._id || user.id,
                    name: user.ho_ten || user.ten_dang_nhap || '',
                    ho_ten: user.ho_ten,
                    ten_dang_nhap: user.ten_dang_nhap,
                    email: user.email,
                    phone: user.so_dien_thoai || 'N/A',
                    so_dien_thoai: user.so_dien_thoai,
                    dia_chi: user.dia_chi,
                    status: user.trang_thai || 'active'
                }));
            
            case 'orders':
                const ordersResponse = await apiCall('/order');
                console.log('Orders API Response:', ordersResponse);
                
                // Handle different response formats
                let orders = [];
                
                if (ordersResponse.success && Array.isArray(ordersResponse.data)) {
                    orders = ordersResponse.data;
                } else if (Array.isArray(ordersResponse)) {
                    orders = ordersResponse;
                } else if (ordersResponse.data && Array.isArray(ordersResponse.data)) {
                    orders = ordersResponse.data;
                }
                
                console.log('Parsed orders:', orders);
                
                // Transform orders data
                return orders.map(order => {
                    // Handle user info - ưu tiên lấy từ order.user (đã populate), sau đó mới từ order.user_id
                    let customerId = '';
                    let customerName = 'N/A';
                    let customerPhone = '';
                    
                    if (order.user) {
                        // Backend đã populate và trả về trong field user
                        customerId = order.user._id || order.user_id || '';
                        customerName = order.user.ho_ten || order.user.ten_dang_nhap || order.user.email || 'N/A';
                        customerPhone = order.user.so_dien_thoai || '';
                    } else if (order.user_id) {
                        // Fallback: nếu user_id là object (populated)
                        if (typeof order.user_id === 'object' && order.user_id !== null) {
                            customerId = order.user_id._id || order.user_id.id || '';
                            customerName = order.user_id.ho_ten || order.user_id.ten_dang_nhap || order.user_id.email || customerId.toString();
                            customerPhone = order.user_id.so_dien_thoai || '';
                        } else {
                            // Chỉ là ID string, không có thông tin
                            customerId = order.user_id.toString();
                            customerName = order.user_id.toString();
                        }
                    }
                    
                    return {
                        id: order._id || order.id,
                        _id: order._id || order.id,
                        customer: customerName,
                        customerId,
                        customerPhone,
                        items: order.items || [],
                        total: order.tong_tien || 0,
                        date: order.createdAt || order.updatedAt || new Date(),
                        status: order.trang_thai || 'pending',
                        address: order.dia_chi_giao_hang || '',
                        phone: order.so_dien_thoai || customerPhone || ''
                    };
                });
            
            case 'products':
                const productsResponse = await apiCall('/product');
                // Handle both response formats
                let products = [];
                if (productsResponse.success && Array.isArray(productsResponse.products)) {
                    products = productsResponse.products;
                } else if (Array.isArray(productsResponse)) {
                    products = productsResponse;
                } else if (productsResponse.products && Array.isArray(productsResponse.products)) {
                    products = productsResponse.products;
                }
                
                // Transform products data
                return products.map(product => ({
                    id: product._id,
                    _id: product._id,
                    ten_san_pham: product.ten_san_pham,
                    thuong_hieu: product.thuong_hieu,
                    danh_muc: product.danh_muc,
                    gia_goc: product.gia_goc,
                    gia_khuyen_mai: product.gia_khuyen_mai,
                    so_luong_ton: product.so_luong_ton,
                    so_luong_da_ban: product.so_luong_da_ban,
                    trang_thai: product.trang_thai,
                    hinh_anh: product.hinh_anh,
                    mo_ta: product.mo_ta,
                    kich_thuoc: product.kich_thuoc,
                    danh_gia: product.danh_gia
                }));
            
            case 'carts':
                const cartsResponse = await apiCall('/cart');
                console.log('Carts API Response:', cartsResponse);
                
                // Handle response format
                let carts = [];
                if (cartsResponse.success && Array.isArray(cartsResponse.data)) {
                    carts = cartsResponse.data;
                } else if (Array.isArray(cartsResponse)) {
                    carts = cartsResponse;
                } else if (cartsResponse.data && Array.isArray(cartsResponse.data)) {
                    carts = cartsResponse.data;
                }
                
                console.log('Parsed carts:', carts);
                
                // Transform carts data
                return carts.map(cart => {
                    // Handle user_id - can be ObjectId string or populated object
                    let customerName = 'N/A';
                    if (cart.user_id) {
                        if (typeof cart.user_id === 'object' && cart.user_id !== null) {
                            customerName = cart.user_id.ho_ten || cart.user_id.email || cart.user_id.toString();
                        } else {
                            customerName = cart.user_id.toString();
                        }
                    }
                    
                    // Calculate total
                    const total = cart.items?.reduce((sum, item) => {
                        return sum + ((item.gia || 0) * (item.so_luong || 0));
                    }, 0) || 0;
                    
                    return {
                        id: cart._id || cart.id,
                        _id: cart._id || cart.id,
                        customer: customerName,
                        customerId: typeof cart.user_id === 'object' ? (cart.user_id?._id || cart.user_id) : cart.user_id,
                        items: cart.items || [],
                        total: total,
                        itemCount: cart.items?.length || 0,
                        totalQuantity: cart.items?.reduce((sum, item) => sum + (item.so_luong || 0), 0) || 0,
                        date: cart.createdAt || cart.updatedAt || new Date()
                    };
                });
            
            default:
                return [];
        }
    } catch (error) {
        console.error(`Error loading ${type}:`, error);
        // Return empty array on error
        return [];
    }
}

// Load data with loading state
async function loadDataWithLoading(type, callback) {
    try {
        const data = await loadData(type);
        if (callback) callback(data, null);
        return data;
    } catch (error) {
        if (callback) callback(null, error);
        return [];
    }
}

// Format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

// Format date
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}

// Show loading indicator
function showLoading(elementId) {
    const table = document.getElementById(elementId);
    if (table) {
        const tbody = table.querySelector('tbody');
        if (tbody) {
            const colCount = table.querySelectorAll('thead tr th').length;
            tbody.innerHTML = `<tr><td colspan="${colCount}" style="text-align: center; padding: 20px;">Đang tải dữ liệu...</td></tr>`;
        }
    }
}

// Show error message
function showError(elementId, message) {
    const table = document.getElementById(elementId);
    if (table) {
        const tbody = table.querySelector('tbody');
        if (tbody) {
            const colCount = table.querySelectorAll('thead tr th').length;
            tbody.innerHTML = `<tr><td colspan="${colCount}" style="text-align: center; padding: 20px; color: #dc3545;">${message}</td></tr>`;
        }
    }
}

