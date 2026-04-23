const API_BASE = '/api/v1';
let selectedRoomId = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    refreshData();
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('roomForm').addEventListener('submit', handleAddRoom);
    document.getElementById('sensorForm').addEventListener('submit', handleAddSensor);
}

async function refreshData() {
    try {
        const response = await fetch(`${API_BASE}/rooms`);
        const rooms = await response.json();
        renderRooms(rooms);
    } catch (error) {
        console.error('Error fetching rooms:', error);
    }
}

function renderRooms(rooms) {
    const container = document.getElementById('roomList');
    if (rooms.length === 0) {
        container.innerHTML = '<div class="empty-state">No rooms found. Add one to get started.</div>';
        return;
    }

    container.innerHTML = rooms.map((room, index) => `
        <div class="room-card ${selectedRoomId === room.id ? 'active' : ''}" 
             onclick="selectRoom('${room.id}', '${room.name}')"
             style="animation-delay: ${index * 0.1}s">
            <div class="room-id">${room.id}</div>
            <div class="room-name">${room.name}</div>
            <div class="room-stats">
                <span>Cap: ${room.capacity}</span>
                <span>Sensors: ${room.sensorIds ? room.sensorIds.length : 0}</span>
            </div>
        </div>
    `).join('');
}

async function selectRoom(roomId, roomName) {
    selectedRoomId = roomId;
    document.getElementById('sensorTitle').innerText = `${roomName} Sensors`;
    document.getElementById('sensorControls').style.display = 'block';
    
    // Refresh room list to show active state
    refreshData();
    loadSensors(roomId);
}

async function loadSensors(roomId) {
    try {
        const response = await fetch(`${API_BASE}/sensors`);
        const allSensors = await response.json();
        const filtered = allSensors.filter(s => s.roomId === roomId);
        renderSensors(filtered);
    } catch (error) {
        console.error('Error fetching sensors:', error);
    }
}

function renderSensors(sensors) {
    const container = document.getElementById('sensorList');
    if (sensors.length === 0) {
        container.innerHTML = '<div class="empty-state">No sensors registered in this room.</div>';
        return;
    }

    container.innerHTML = sensors.map(sensor => `
        <div class="sensor-card">
            <div class="sensor-info">
                <h4>${sensor.id}</h4>
                <span class="sensor-type">${sensor.type}</span>
                <span class="status-badge ${sensor.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}">
                    ${sensor.status}
                </span>
            </div>
            <div class="sensor-value">
                ${sensor.currentValue}${getUnit(sensor.type)}
            </div>
        </div>
    `).join('');
}

function getUnit(type) {
    switch (type) {
        case 'Temperature': return '°C';
        case 'Humidity': return '%';
        case 'CO2': return ' ppm';
        case 'Light': return ' lux';
        case 'Occupancy': return ' ppl';
        default: return '';
    }
}

async function handleAddRoom(e) {
    e.preventDefault();
    const room = {
        id: document.getElementById('newRoomId').value,
        name: document.getElementById('newRoomName').value,
        capacity: parseInt(document.getElementById('newRoomCapacity').value)
    };

    try {
        const response = await fetch(`${API_BASE}/rooms`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(room)
        });

        if (response.ok) {
            hideModals();
            refreshData();
            document.getElementById('roomForm').reset();
        } else {
            alert('Error adding room. Possible duplicate ID.');
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

async function handleAddSensor(e) {
    e.preventDefault();
    if (!selectedRoomId) return;

    const sensor = {
        id: document.getElementById('newSensorId').value,
        type: document.getElementById('newSensorType').value,
        status: 'ACTIVE',
        currentValue: parseFloat(document.getElementById('newSensorValue').value),
        roomId: selectedRoomId
    };

    try {
        const response = await fetch(`${API_BASE}/sensors`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(sensor)
        });

        if (response.ok) {
            hideModals();
            loadSensors(selectedRoomId);
            refreshData();
            document.getElementById('sensorForm').reset();
        } else {
            alert('Error adding sensor.');
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

function showAddRoomModal() {
    document.getElementById('roomModal').style.display = 'flex';
}

function showAddSensorModal() {
    document.getElementById('sensorModal').style.display = 'flex';
}

function hideModals() {
    document.querySelectorAll('.modal').forEach(m => m.style.display = 'none');
}

// Close modal on click outside
window.onclick = function(event) {
    if (event.target.className === 'modal') {
        hideModals();
    }
}
