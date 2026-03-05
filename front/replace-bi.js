const fs = require('fs');

function replaceIcons(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    content = content.replace(/fas fa-stethoscope/g, 'bi bi-heart-pulse');
    content = content.replace(/fas fa-calendar-alt/g, 'bi bi-calendar3');
    content = content.replace(/fas fa-th-large/g, 'bi bi-grid-fill');
    content = content.replace(/fas fa-hourglass-half/g, 'bi bi-hourglass-split');
    content = content.replace(/fas fa-clock/g, 'bi bi-clock');
    content = content.replace(/fas fa-shield-alt/g, 'bi bi-shield-check');
    content = content.replace(/fas fa-exclamation-triangle/g, 'bi bi-exclamation-triangle');
    content = content.replace(/fas fa-ban/g, 'bi bi-slash-circle');
    content = content.replace(/fas fa-calendar-times/g, 'bi bi-calendar-x');
    content = content.replace(/fas fa-calendar-day/g, 'bi bi-calendar-event');
    content = content.replace(/fas fa-calendar/g, 'bi bi-calendar');
    content = content.replace(/fas fa-times-circle/g, 'bi bi-x-circle');
    content = content.replace(/fas fa-user-injured/g, 'bi bi-person');
    content = content.replace(/fas fa-user/g, 'bi bi-person');
    content = content.replace(/fas fa-video/g, 'bi bi-camera-video');
    content = content.replace(/fas fa-building/g, 'bi bi-building');
    content = content.replace(/fas fa-exclamation/g, 'bi bi-exclamation-lg');
    content = content.replace(/fas fa-check-circle/g, 'bi bi-check-circle');
    content = content.replace(/fas fa-check/g, 'bi bi-check-lg');
    content = content.replace(/fas fa-trash-alt/g, 'bi bi-trash');
    content = content.replace(/fas fa-eye/g, 'bi bi-eye');
    content = content.replace(/fas fa-arrow-left/g, 'bi bi-arrow-left');
    content = content.replace(/fas fa-envelope/g, 'bi bi-envelope');
    content = content.replace(/fas fa-bell/g, 'bi bi-bell');
    content = content.replace(/fas fa-file-medical/g, 'bi bi-file-earmark-medical');
    content = content.replace(/fas fa-heartbeat/g, 'bi bi-heart-pulse');
    content = content.replace(/fas fa-pills/g, 'bi bi-capsule');
    content = content.replace(/fas fa-save/g, 'bi bi-save');

    content = content.replace(/<i class="fi "/g, '<i class="bi"');
    content = content.replace(/<i class="fi/g, '<i class="bi');

    fs.writeFileSync(filePath, content);
    console.log('Processed', filePath);
}

replaceIcons('src/app/backoffice/doctor-appointments/doctor-appointments.html');
replaceIcons('src/app/backoffice/doctor-appointment-detail/doctor-appointment-detail.html');
