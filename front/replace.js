const fs = require('fs');

function replaceIcons(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    content = content.replace(/fi fi-rr-stethoscope/g, 'fas fa-stethoscope');
    content = content.replace(/fi fi-rr-calendar-lines/g, 'fas fa-calendar-alt');
    content = content.replace(/fi fi-rr-apps/g, 'fas fa-th-large');
    content = content.replace(/fi fi-rr-hourglass-end/g, 'fas fa-hourglass-half');
    content = content.replace(/fi fi-rr-time-quarter-to/g, 'fas fa-clock');
    content = content.replace(/fi fi-rr-shield-check/g, 'fas fa-shield-alt');
    content = content.replace(/fi fi-rr-triangle-warning/g, 'fas fa-exclamation-triangle');
    content = content.replace(/fi fi-rr-ban/g, 'fas fa-ban');
    content = content.replace(/fi fi-rr-calendar-xmark/g, 'fas fa-calendar-times');
    content = content.replace(/fi fi-rr-calendar-pen/g, 'fas fa-calendar-day');
    content = content.replace(/fi fi-rr-calendar/g, 'fas fa-calendar');
    content = content.replace(/fi fi-rr-cross-circle/g, 'fas fa-times-circle');
    content = content.replace(/fi fi-rr-video-camera-alt/g, 'fas fa-video');
    content = content.replace(/fi fi-rr-building/g, 'fas fa-building');
    content = content.replace(/fi fi-rr-exclamation/g, 'fas fa-exclamation');
    content = content.replace(/fi fi-rr-check-circle/g, 'fas fa-check-circle');
    content = content.replace(/fi fi-rr-check/g, 'fas fa-check');
    content = content.replace(/fi fi-rr-trash/g, 'fas fa-trash-alt');
    content = content.replace(/fi fi-rr-eye/g, 'fas fa-eye');
    content = content.replace(/fi fi-rr-arrow-left/g, 'fas fa-arrow-left');
    content = content.replace(/fi fi-rr-envelope/g, 'fas fa-envelope');
    content = content.replace(/fi fi-rr-bell-ring/g, 'fas fa-bell');
    content = content.replace(/fi fi-rr-file-medical-alt/g, 'fas fa-file-medical');
    content = content.replace(/fi fi-rr-pulse/g, 'fas fa-heartbeat');
    content = content.replace(/fi fi-rr-capsules/g, 'fas fa-pills');
    content = content.replace(/fi fi-rr-disk/g, 'fas fa-save');
    content = content.replace(/fi fi-rr-clock/g, 'fas fa-clock');
    content = content.replace(/fi fi-rr-user/g, 'fas fa-user');

    // also handle just 'fi ' cases in ngClass
    content = content.replace(/'fi-rr-hourglass-end'/g, "'fas fa-hourglass-half'");
    content = content.replace(/'fi-rr-check-circle'/g, "'fas fa-check-circle'");
    content = content.replace(/'fi-rr-cross-circle'/g, "'fas fa-times-circle'");
    content = content.replace(/'fi-rr-calendar-pen'/g, "'fas fa-calendar-day'");
    content = content.replace(/'fi-rr-video-camera-alt'/g, "'fas fa-video'");
    content = content.replace(/'fi-rr-building'/g, "'fas fa-building'");
    content = content.replace(/<i class="fi "/g, '<i class="fas"');

    fs.writeFileSync(filePath, content);
    console.log('Processed', filePath);
}
replaceIcons('src/app/backoffice/doctor-appointments/doctor-appointments.html');
replaceIcons('src/app/backoffice/doctor-appointment-detail/doctor-appointment-detail.html');
