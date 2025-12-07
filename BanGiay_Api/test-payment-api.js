// Script test API payment
const axios = require('axios');

const API_BASE_URL = 'http://localhost:3000/api';

async function testPaymentAPI() {
  console.log('=== Testing Payment API ===\n');

  const testPaymentData = {
    ten_chu_the: "Nguyen Van A",
    so_the: "1234567890123456",
    loai_the: "credit_card",
    ten_san_pham: "Giày thể thao",
    gia_san_pham: "500000₫",
    so_luong: 1,
    kich_thuoc: "42",
    ngay_het_han: "12/25"
  };

  try {
    console.log('1. Testing POST /api/payment');
    console.log('Request data:', JSON.stringify(testPaymentData, null, 2));
    
    const response = await axios.post(`${API_BASE_URL}/payment`, testPaymentData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    console.log('\n✅ Response Status:', response.status);
    console.log('Response Data:', JSON.stringify(response.data, null, 2));
    
    if (response.data.success) {
      console.log('\n✅ Payment created successfully!');
      console.log('Payment ID:', response.data.data._id);
    }
  } catch (error) {
    console.error('\n❌ Error:', error.message);
    if (error.response) {
      console.error('Status:', error.response.status);
      console.error('Data:', error.response.data);
    }
  }

  try {
    console.log('\n\n2. Testing GET /api/payment');
    const getResponse = await axios.get(`${API_BASE_URL}/payment`);
    console.log('✅ Response Status:', getResponse.status);
    console.log('Total Payments:', getResponse.data.data?.length || 0);
    console.log('Payments:', JSON.stringify(getResponse.data.data, null, 2));
  } catch (error) {
    console.error('\n❌ Error:', error.message);
    if (error.response) {
      console.error('Status:', error.response.status);
      console.error('Data:', error.response.data);
    }
  }
}

// Run test
testPaymentAPI();

