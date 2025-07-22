// Configura la URL de tu tabla y tu API Key de Supabase
const SUPABASE_URL = "https://kldhwzmvasgzlbbaxicl.supabase.co/rest/v1/locations?select=latitude,longitude";
const API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtsZGh3em12YXNnemxiYmF4aWNsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTI2MDY2NTYsImV4cCI6MjA2ODE4MjY1Nn0.RGUgnqrltINpoEKr6EfZ8Ry7DRwufZanjpPxyGc5hKo";

// Inicializa el mapa con Leaflet (centrado por defecto)
const map = L.map('map').setView([0, 0], 2);

// Carga el mapa base desde OpenStreetMap
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);

// Consulta las ubicaciones desde Supabase
fetch(SUPABASE_URL, {
  headers: {
    apikey: API_KEY,
    Authorization: `Bearer ${API_KEY}`,
  }
})
.then(res => res.json())
.then(data => {
  if (data.length === 0) {
    alert("No se encontraron ubicaciones.");
    return;
  }

  data.forEach((punto, index) => {
    const latlng = [punto.latitude, punto.longitude];
    L.marker(latlng).addTo(map).bindPopup(`Ubicación ${index + 1}`);
  });

  // Centrar el mapa en la primera ubicación
  const primera = data[0];
  map.setView([primera.latitude, primera.longitude], 15);
})
.catch(err => {
  console.error("Error consultando Supabase:", err);
  alert("No se pudo cargar el mapa.");
});