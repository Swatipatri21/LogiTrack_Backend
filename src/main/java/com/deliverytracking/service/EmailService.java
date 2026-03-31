package com.deliverytracking.service;

import com.deliverytracking.entity.Shipment;
import com.deliverytracking.entity.User;
import com.deliverytracking.enums.ShipmentStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;

	@Value("${app.mail.from}")
	private String fromEmail;

	@Value("${app.mail.fromName}")
	private String fromName;

	// ── CORE SEND METHOD ──────────────────────────────────────
	@Async
	@Transactional
	public void sendEmail(String to, String subject, String htmlBody) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(fromEmail, fromName);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlBody, true);
			mailSender.send(message);
			logger.info("✅ Email sent to: {} | Subject: {}", to, subject);
		} catch (MessagingException | java.io.UnsupportedEncodingException e) {
			logger.error("❌ Failed to send email to {}: {}", to, e.getMessage());
		}
	}

	// ── 1. WELCOME EMAIL ──────────────────────────────────────
	@Async
	@Transactional
	public void sendWelcomeEmail(User user) {
		String subject = "Welcome to LogiTrack! 🚚";
		String body = """
				<div style="font-family:Arial,sans-serif;
				            max-width:600px;margin:auto;
				            padding:20px;
				            border:1px solid #E0E7EF;
				            border-radius:12px;">
				    <div style="background:linear-gradient(
				                135deg,#1565C0,#00ACC1);
				                padding:24px;border-radius:8px;
				                text-align:center;margin-bottom:24px;">
				        <h1 style="color:white;margin:0;
				                   font-size:28px;">
				            🚚 LogiTrack
				        </h1>
				        <p style="color:rgba(255,255,255,0.9);
				                   margin:8px 0 0;">
				            Your Delivery Management Platform
				        </p>
				    </div>
				    <h2 style="color:#0D1B2A;">
				        Welcome, %s! 👋
				    </h2>
				    <p style="color:#37474F;">
				        Your account has been created successfully.
				    </p>
				    <table style="width:100%%;
				                  border-collapse:collapse;
				                  margin:16px 0;">
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;
				                       color:#37474F;width:40%%;">
				                Name
				            </td>
				            <td style="padding:10px;color:#0D1B2A;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;
				                       color:#37474F;">
				                Email
				            </td>
				            <td style="padding:10px;color:#0D1B2A;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;
				                       color:#37474F;">
				                Role
				            </td>
				            <td style="padding:10px;
				                       color:#1565C0;
				                       font-weight:600;">
				                %s
				            </td>
				        </tr>
				    </table>
				    <p style="color:#37474F;">
				        You can now login and start using LogiTrack.
				    </p>
				    <p style="color:#78909C;font-size:12px;
				               margin-top:24px;">
				        If you did not create this account,
				        please contact support immediately.
				    </p>
				</div>
				""".formatted(user.getName(), user.getName(), user.getEmail(), user.getRole().name());
//        sendEmail(user.getEmail(), subject, body);
	}

	// ── 2. LOGIN ALERT EMAIL ──────────────────────────────────
	@Async
	@Transactional
	public void sendLoginAlertEmail(User user) {
		String subject = "New Login Alert — LogiTrack 🔐";
		String body = """
				<div style="font-family:Arial,sans-serif;
				            max-width:600px;margin:auto;
				            padding:20px;
				            border:1px solid #E0E7EF;
				            border-radius:12px;">
				    <h2 style="color:#0D1B2A;">
				        Login Alert 🔐
				    </h2>
				    <p style="color:#37474F;">
				        Hi <strong>%s</strong>,
				        a new login was detected on your account.
				    </p>
				    <table style="width:100%%;
				                  border-collapse:collapse;
				                  margin:16px 0;">
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;width:40%%;">
				                Email
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Time
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Role
				            </td>
				            <td style="padding:10px;
				                       color:#1565C0;
				                       font-weight:600;">
				                %s
				            </td>
				        </tr>
				    </table>
				    <p style="color:#C62828;font-weight:600;">
				        If this was not you, please change your
				        password immediately.
				    </p>
				</div>
				""".formatted(user.getName(), user.getEmail(), java.time.LocalDateTime.now().toString(),
				user.getRole().name());
//        sendEmail(user.getEmail(), subject, body);
	}

	// ── TASK ASSIGNED EMAIL ───────────────────────────────────
	@Async
	@Transactional
	public void sendTaskAssignedEmail(User staff, Shipment shipment, ShipmentStatus task, String city,
			LocalDateTime deadline, String instructions, int attempt) {
		String subject = attempt > 1 ? "🔄 Reassigned Task — " + shipment.getTrackingId()
				: "📋 New Task — " + shipment.getTrackingId();

		String urgencyColor = attempt > 1 ? "#C62828" : "#1565C0";
		String attemptNote = attempt > 1
				? "<div style='background:#FFEBEE;" + "border-left:4px solid #C62828;" + "padding:12px;margin:12px 0;'>"
						+ "<strong style='color:#C62828;'>" + "⚠️ Reassigned Task (Attempt #" + attempt
						+ ")</strong><br>" + "<span style='color:#37474F;font-size:13px;'>"
						+ "Previous staff did not complete on time." + "</span></div>"
				: "";

		String body = """
				<div style="font-family:Arial,sans-serif;
				            max-width:600px;margin:auto;
				            padding:20px;
				            border:2px solid %s;
				            border-radius:12px;">
				    <div style="background:%s;padding:20px;
				                border-radius:8px;
				                text-align:center;
				                margin-bottom:20px;">
				        <h1 style="color:white;margin:0;
				                   font-size:22px;">
				            📋 Task Assigned
				        </h1>
				    </div>
				    %s
				    <p style="color:#37474F;">
				        Hi <strong>%s</strong>,
				        you have a new task to complete.
				    </p>
				    <div style="background:#E8F0FE;
				                border-left:4px solid #1565C0;
				                padding:16px;border-radius:4px;
				                margin:16px 0;">
				        <p style="margin:0 0 4px;font-size:13px;
				                   color:#78909C;">
				            Your Task
				        </p>
				        <p style="margin:0;font-size:22px;
				                   font-weight:700;color:#1565C0;">
				            Update Status To: %s
				        </p>
				    </div>
				    <table style="width:100%%;
				                  border-collapse:collapse;
				                  margin:16px 0;">
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;width:40%%;">
				                Tracking ID
				            </td>
				            <td style="padding:10px;
				                       color:#1565C0;
				                       font-weight:700;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Route
				            </td>
				            <td style="padding:10px;">
				                %s → %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Your Location
				            </td>
				            <td style="padding:10px;">
				                📍 %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Instructions
				            </td>
				            <td style="padding:10px;
				                       font-style:italic;
				                       color:#37474F;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#FFEBEE;
				                       font-weight:600;
				                       color:#C62828;">
				                ⏰ Complete By
				            </td>
				            <td style="padding:10px;
				                       color:#C62828;
				                       font-weight:700;">
				                %s
				            </td>
				        </tr>
				    </table>
				    <div style="background:#FFF3E0;
				                border-left:4px solid #F57C00;
				                padding:12px;border-radius:4px;">
				        <strong style="color:#F57C00;">
				            ⚠️ Important:
				        </strong>
				        <span style="color:#37474F;">
				            If you do not complete this task by
				            the deadline, it will be automatically
				            reassigned to another staff member.
				        </span>
				    </div>
				</div>
				""".formatted(urgencyColor, urgencyColor, attemptNote, staff.getName(), task.name().replace("_", " "),
				shipment.getTrackingId(), shipment.getOrigin(), shipment.getDestination(),
				city != null ? city : "Your Zone", instructions, deadline != null ? deadline.toString() : "ASAP");

//        sendEmail(staff.getEmail(), subject, body);
	}

	// ── REASSIGNMENT ALERT TO ADMIN ───────────────────────────
//    @Async
//    @Transactional
//    public void sendReassignmentAlertEmail(
//            User admin,
//            ShipmentAssignment task) {
//
//        String subject = "⚠️ Task Auto-Reassigned — " +
//                task.getShipment().getTrackingId();
//
//        String body = """
//            <div style="font-family:Arial,sans-serif;
//                        max-width:600px;margin:auto;
//                        padding:20px;
//                        border:2px solid #F57C00;
//                        border-radius:12px;">
//                <h2 style="color:#F57C00;">
//                    ⚠️ Task Auto-Reassigned
//                </h2>
//                <p style="color:#37474F;">
//                    A task was not completed on time and
//                    has been automatically reassigned.
//                </p>
//                <table style="width:100%%;
//                              border-collapse:collapse;
//                              margin:16px 0;">
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;width:40%%;">
//                            Tracking ID
//                        </td>
//                        <td style="padding:10px;
//                                   color:#1565C0;
//                                   font-weight:700;">
//                            %s
//                        </td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;">
//                            Required Status
//                        </td>
//                        <td style="padding:10px;">
//                            %s
//                        </td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;">
//                            Was Assigned To
//                        </td>
//                        <td style="padding:10px;
//                                   color:#C62828;">
//                            %s
//                        </td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;">
//                            Deadline Was
//                        </td>
//                        <td style="padding:10px;">
//                            %s
//                        </td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;">
//                            Attempt #
//                        </td>
//                        <td style="padding:10px;">
//                            %d
//                        </td>
//                    </tr>
//                </table>
//                <p style="color:#78909C;font-size:13px;">
//                    The task has been assigned to the next
//                    available staff member automatically.
//                    Please monitor if the issue persists.
//                </p>
//            </div>
//            """.formatted(
//                task.getShipment().getTrackingId(),
//                task.getRequiredStatus().name()
//                        .replace("_", " "),
//                task.getAssignedTo().getEmail(),
//                task.getDeadlineAt() != null
//                        ? task.getDeadlineAt().toString()
//                        : "N/A",
//                task.getAttemptNumber() != null
//                        ? task.getAttemptNumber() : 1
//        );
//
//        sendEmail(admin.getEmail(), subject, body);
//    }

	// ── 3. SHIPMENT CREATED EMAIL ─────────────────────────────
//    @Async
//    @Transactional
//    public void sendShipmentCreatedEmail(Shipment shipment) {
//        String subject = "Shipment Created — Tracking ID: "
//                + shipment.getTrackingId() + " 📦";
//        String body = """
//            <div style="font-family:Arial,sans-serif;
//                        max-width:600px;margin:auto;
//                        padding:20px;
//                        border:1px solid #E0E7EF;
//                        border-radius:12px;">
//                <div style="background:linear-gradient(
//                            135deg,#1565C0,#00ACC1);
//                            padding:24px;border-radius:8px;
//                            text-align:center;
//                            margin-bottom:24px;">
//                    <h1 style="color:white;margin:0;">
//                        📦 Shipment Created
//                    </h1>
//                </div>
//                <p style="color:#37474F;">
//                    Dear <strong>%s</strong>,
//                    your shipment has been created successfully.
//                </p>
//                <div style="background:#E8F0FE;
//                            border-left:4px solid #1565C0;
//                            padding:16px;border-radius:4px;
//                            margin:16px 0;">
//                    <p style="margin:0;font-size:13px;
//                               color:#37474F;">
//                        Tracking ID
//                    </p>
//                    <p style="margin:4px 0 0;
//                               font-size:24px;
//                               font-weight:700;
//                               color:#1565C0;
//                               letter-spacing:2px;">
//                        %s
//                    </p>
//                </div>
//                <table style="width:100%%;
//                              border-collapse:collapse;
//                              margin:16px 0;">
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;
//                                   color:#37474F;width:40%%;">
//                            From
//                        </td>
//                        <td style="padding:10px;">%s</td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;
//                                   color:#37474F;">
//                            To
//                        </td>
//                        <td style="padding:10px;">%s</td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;
//                                   color:#37474F;">
//                            Receiver
//                        </td>
//                        <td style="padding:10px;">%s</td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;
//                                   color:#37474F;">
//                            Weight
//                        </td>
//                        <td style="padding:10px;">%s kg</td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px;
//                                   background:#F4F7FB;
//                                   font-weight:600;
//                                   color:#37474F;">
//                            Expected Delivery
//                        </td>
//                        <td style="padding:10px;
//                                   color:#00897B;
//                                   font-weight:600;">
//                            %s
//                        </td>
//                    </tr>
//                </table>
//                <p style="color:#37474F;">
//                    Use your Tracking ID
//                    <strong style="color:#1565C0;">%s</strong>
//                    to track your shipment anytime.
//                </p>
//            </div>
//            """.formatted(
//                shipment.getSenderName(),
//                shipment.getSenderEmail(),
//                shipment.getTrackingId(),
//                shipment.getOrigin(),
//                shipment.getDestination(),
//                shipment.getReceiverName(),
//                shipment.getWeight(),
//                shipment.getExpectedDeliveryDate() != null
//                        ? shipment.getExpectedDeliveryDate()
//                        : "Calculating...",
//                shipment.getTrackingId()
//        );
//        sendEmail(shipment.getSenderEmail(),
//                subject, body);
//    }

	// ── RESCHEDULE DELIVERY EMAIL ─────────────────────────────
	@Async

	public void sendRescheduleDeliveryEmail(Shipment shipment, LocalDate newDeliveryDate, String reason) {
		String subject = "🔄 Delivery Rescheduled — " + shipment.getTrackingId();

		String formattedDate = newDeliveryDate != null
				? newDeliveryDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
				: "To Be Confirmed";

		String body = """
				<div style="font-family:Arial,sans-serif;
				            max-width:600px;margin:auto;
				            padding:20px;
				            border:2px solid #F57C00;
				            border-radius:12px;">

				    <div style="background:#F57C00;
				                padding:24px;border-radius:8px;
				                text-align:center;
				                margin-bottom:24px;">
				        <h1 style="color:white;margin:0;font-size:24px;">
				            🔄 Delivery Rescheduled
				        </h1>
				    </div>

				    <p style="color:#37474F;">
				        Dear <strong>%s</strong>,
				        your shipment delivery has been rescheduled.
				        We apologize for any inconvenience caused.
				    </p>

				    <div style="background:#FFF3E0;
				                border-left:4px solid #F57C00;
				                padding:16px;border-radius:4px;
				                margin:16px 0;">
				        <p style="margin:0;font-size:13px;color:#37474F;">
				            New Delivery Date
				        </p>
				        <p style="margin:4px 0 0;font-size:24px;
				                   font-weight:700;color:#F57C00;">
				            📅 %s
				        </p>
				    </div>

				    <table style="width:100%%;
				                  border-collapse:collapse;
				                  margin:16px 0;">
				        <tr>
				            <td style="padding:10px;background:#F4F7FB;
				                       font-weight:600;width:40%%;">
				                Tracking ID
				            </td>
				            <td style="padding:10px;color:#1565C0;
				                       font-weight:700;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;background:#F4F7FB;
				                       font-weight:600;">
				                Current Status
				            </td>
				            <td style="padding:10px;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;background:#F4F7FB;
				                       font-weight:600;">
				                Reason
				            </td>
				            <td style="padding:10px;color:#37474F;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;background:#F4F7FB;
				                       font-weight:600;">
				                Route
				            </td>
				            <td style="padding:10px;">
				                %s → %s
				            </td>
				        </tr>
				    </table>

				    <div style="background:#FFF3E0;
				                border-left:4px solid #F57C00;
				                padding:12px;border-radius:4px;
				                margin-top:16px;">
				        <strong style="color:#F57C00;">📞 Note:</strong>
				        <span style="color:#37474F;">
				            Please ensure someone is available to
				            receive the package on the new delivery date.
				        </span>
				    </div>

				    <p style="color:#78909C;font-size:12px;margin-top:24px;">
				        Track your shipment anytime using ID:
				        <strong>%s</strong>
				    </p>
				</div>
				""".formatted(shipment.getReceiverName(), formattedDate, shipment.getTrackingId(),
				shipment.getCurrentStatus().name().replace("_", " "), reason != null ? reason : "Operational reasons",
				shipment.getOrigin(), shipment.getDestination(), shipment.getTrackingId());

		// Send to sender/creator
		sendEmail(shipment.getCreatedBy().getEmail(), subject, body);

		// Send to receiver
		if (shipment.getReceiverEmail() != null && !shipment.getReceiverEmail().isBlank()) {
			sendEmail(shipment.getReceiverEmail(), subject, body);
			logger.info("✅ Reschedule email sent to receiver: {}", shipment.getReceiverEmail());
		}
	}

	@Async
	@Transactional
	public void sendShipmentCreatedEmail(Shipment shipment) {

		String subject = "Shipment Created — Tracking ID: " + shipment.getTrackingId() + " 📦";

		String expectedDelivery = shipment.getExpectedDeliveryDate() != null
				? shipment.getExpectedDeliveryDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
				: "";

		String body = """
				<div style="font-family:Arial,sans-serif;
				            max-width:600px;margin:auto;
				            padding:20px;
				            border:1px solid #E0E7EF;
				            border-radius:12px;">

				    <div style="background:linear-gradient(
				                135deg,#1565C0,#00ACC1);
				                padding:24px;border-radius:8px;
				                text-align:center;
				                margin-bottom:24px;">
				        <h1 style="color:white;margin:0;">
				            📦 Shipment Created
				        </h1>
				    </div>

				    <p style="color:#37474F;">
				        Dear <strong>%s</strong>, your shipment has been created successfully.
				    </p>

				    <div style="background:#E8F0FE;
				                border-left:4px solid #1565C0;
				                padding:16px;border-radius:4px;
				                margin:16px 0;">

				        <p style="margin:0;font-size:13px;color:#37474F;">
				            Tracking ID
				        </p>

				        <p style="margin:4px 0 0;
				                   font-size:24px;
				                   font-weight:700;
				                   color:#1565C0;
				                   letter-spacing:2px;">
				            %s
				        </p>

				    </div>

				    <table style="width:100%%;
				                  border-collapse:collapse;
				                  margin:16px 0;">

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;width:40%%;">
				                From
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;">
				                To
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;">
				                Receiver
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;">
				                Weight
				            </td>
				            <td style="padding:10px;">%s kg</td>
				        </tr>

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;">
				                Expected Delivery
				            </td>
				            <td style="padding:10px;color:#00897B;font-weight:600;">
				                %s
				            </td>
				        </tr>

				    </table>

				    <p style="color:#37474F;">
				        Use your Tracking ID
				        <strong style="color:#1565C0;">%s</strong>
				        to track your shipment anytime.
				    </p>

				</div>
				""".formatted(shipment.getSenderName(), shipment.getTrackingId(), shipment.getOrigin(),
				shipment.getDestination(), shipment.getReceiverName(), shipment.getWeight(), expectedDelivery,
				shipment.getTrackingId());

		sendEmail(shipment.getSenderEmail(), subject, body);
	}

	@Async
	@Transactional
	public void sendShipmentReceiverEmail(Shipment shipment) {

		String subject = "Package on the Way — Tracking ID: " + shipment.getTrackingId() + " 📦";

		String expectedDelivery = shipment.getExpectedDeliveryDate() != null
				? shipment.getExpectedDeliveryDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
				: "";

		String body = """
				<div style="font-family:Arial,sans-serif;
				            max-width:600px;margin:auto;
				            padding:20px;
				            border:1px solid #E0E7EF;
				            border-radius:12px;">

				    <div style="background:linear-gradient(
				                135deg,#2E7D32,#66BB6A);
				                padding:24px;border-radius:8px;
				                text-align:center;
				                margin-bottom:24px;">
				        <h1 style="color:white;margin:0;">
				            🚚 Shipment on the Way
				        </h1>
				    </div>

				    <p style="color:#37474F;">
				        Dear <strong>%s</strong>,
				        a shipment has been sent to you and is now in our delivery network.
				    </p>

				    <div style="background:#E8F5E9;
				                border-left:4px solid #2E7D32;
				                padding:16px;border-radius:4px;
				                margin:16px 0;">

				        <p style="margin:0;font-size:13px;color:#37474F;">
				            Tracking ID
				        </p>

				        <p style="margin:4px 0 0;
				                   font-size:24px;
				                   font-weight:700;
				                   color:#2E7D32;
				                   letter-spacing:2px;">
				            %s
				        </p>

				    </div>

				    <table style="width:100%%;
				                  border-collapse:collapse;
				                  margin:16px 0;">

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;width:40%%;">
				                Sender
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;">
				                From
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;">
				                Destination
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;">
				                Weight
				            </td>
				            <td style="padding:10px;">%s kg</td>
				        </tr>

				        <tr>
				            <td style="padding:10px;background:#F4F7FB;font-weight:600;color:#37474F;">
				                Expected Delivery
				            </td>
				            <td style="padding:10px;color:#2E7D32;font-weight:600;">
				                %s
				            </td>
				        </tr>

				    </table>

				    <p style="color:#37474F;">
				        You can track your shipment anytime using
				        Tracking ID
				        <strong style="color:#2E7D32;">%s</strong>
				        on our tracking portal.
				    </p>

				</div>
				""".formatted(shipment.getReceiverName(), shipment.getTrackingId(), shipment.getSenderName(),
				shipment.getOrigin(), shipment.getDestination(), shipment.getWeight(), expectedDelivery,
				shipment.getTrackingId());

		sendEmail(shipment.getReceiverEmail(), subject, body);
	}

	// ── 4. STATUS UPDATE EMAIL ────────────────────────────────
	@Async
	@Transactional
	public void sendStatusUpdateEmail(Shipment shipment, ShipmentStatus newStatus, String remarks, String location) {
		String subject = getSubjectForStatus(newStatus, shipment.getTrackingId());
		String color = getColorForStatus(newStatus);
		String emoji = getEmojiForStatus(newStatus);

		String body = """
				<div style="font-family:Arial,sans-serif;
				            max-width:600px;margin:auto;
				            padding:20px;
				            border:1px solid #E0E7EF;
				            border-radius:12px;">
				    <div style="background:%s;
				                padding:24px;border-radius:8px;
				                text-align:center;
				                margin-bottom:24px;">
				        <h1 style="color:white;margin:0;
				                   font-size:24px;">
				            %s Shipment Update
				        </h1>
				        <p style="color:rgba(255,255,255,0.9);
				                   margin:8px 0 0;
				                   font-size:18px;
				                   font-weight:700;">
				            %s
				        </p>
				    </div>
				    <p style="color:#37474F;">
				        Dear <strong>%s</strong>,
				        your shipment status has been updated.
				    </p>
				    <table style="width:100%%;
				                  border-collapse:collapse;
				                  margin:16px 0;">
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;
				                       width:40%%;">
				                Tracking ID
				            </td>
				            <td style="padding:10px;
				                       color:#1565C0;
				                       font-weight:700;
				                       letter-spacing:1px;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Current Status
				            </td>
				            <td style="padding:10px;
				                       font-weight:700;">
				                %s %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Location
				            </td>
				            <td style="padding:10px;">
				                📍 %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Remarks
				            </td>
				            <td style="padding:10px;
				                       font-style:italic;
				                       color:#37474F;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Route
				            </td>
				            <td style="padding:10px;">
				                %s → %s
				            </td>
				        </tr>
				    </table>
				    %s
				    <p style="color:#78909C;font-size:12px;
				               margin-top:24px;">
				        Track your shipment anytime using ID:
				        <strong>%s</strong>
				    </p>
				</div>
				""".formatted(color, emoji, newStatus.name().replace("_", " "), shipment.getReceiverName(),
				shipment.getTrackingId(), emoji, newStatus.name().replace("_", " "),
				location != null ? location : "N/A", remarks != null ? remarks : "N/A", shipment.getOrigin(),
				shipment.getDestination(), getExtraMessageForStatus(newStatus), shipment.getTrackingId());
//        sendEmail(shipment.getCreatedBy().getEmail(),
//                subject, body);
//        sendEmail(shipment.getCreatedBy().getEmail(), subject, body);
//
//        if (shipment.getReceiverEmail() != null &&
//                !shipment.getReceiverEmail().isBlank()) {
//            sendEmail(shipment.getReceiverEmail(), subject, body);
//            logger.info("✅ Status email also sent to receiver: {}",
//                    shipment.getReceiverEmail());
//        }
	}

	// ── 5. DELAY NOTIFICATION EMAIL ───────────────────────────

	@Async
	public void sendDelayNotificationEmail(Shipment shipment, String reason, LocalDate revisedDate, int delayDays) {

		String subject = "⚠️ Delivery Delay Alert — " + shipment.getTrackingId();

		String formattedRevised = revisedDate != null
				? revisedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
				: "To Be Confirmed";

		// ── SENDER / CREATOR EMAIL BODY ───────────────────────────
		String senderBody = """
				<div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
				            padding:20px;border:2px solid #C62828;border-radius:12px;">

				    <div style="background:linear-gradient(135deg,#C62828,#E53935);
				                padding:24px;border-radius:8px;text-align:center;margin-bottom:24px;">
				        <h1 style="color:white;margin:0;font-size:24px;">⚠️ Delivery Delay Alert</h1>
				        <p style="color:rgba(255,255,255,0.9);margin:8px 0 0;font-size:14px;">
				            Action may be required
				        </p>
				    </div>

				    <p style="color:#37474F;font-size:15px;">
				        Dear <strong>%s</strong>, we regret to inform you that your shipment
				        has encountered a delay.
				    </p>

				    <div style="background:#E8F0FE;border-left:4px solid #1565C0;
				                padding:16px;border-radius:4px;margin:16px 0;">
				        <p style="margin:0;font-size:13px;color:#78909C;">Tracking ID</p>
				        <p style="margin:4px 0 0;font-size:22px;font-weight:700;
				                   color:#1565C0;letter-spacing:2px;">%s</p>
				    </div>

				    <table style="width:100%%;border-collapse:collapse;margin:16px 0;">
				        <tr>
				            <td style="padding:12px;background:#F4F7FB;font-weight:600;
				                       color:#37474F;width:40%%;border-bottom:1px solid #E0E7EF;">
				                Current Status
				            </td>
				            <td style="padding:12px;color:#0D1B2A;border-bottom:1px solid #E0E7EF;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:12px;background:#F4F7FB;font-weight:600;
				                       color:#37474F;border-bottom:1px solid #E0E7EF;">
				                Route
				            </td>
				            <td style="padding:12px;color:#0D1B2A;border-bottom:1px solid #E0E7EF;">
				                %s → %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:12px;background:#FFEBEE;font-weight:600;
				                       color:#C62828;border-bottom:1px solid #E0E7EF;">
				                ⏱ Delayed By
				            </td>
				            <td style="padding:12px;color:#C62828;font-weight:700;
				                       border-bottom:1px solid #E0E7EF;">
				                %d day(s)
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:12px;background:#F4F7FB;font-weight:600;
				                       color:#37474F;border-bottom:1px solid #E0E7EF;">
				                Reason
				            </td>
				            <td style="padding:12px;color:#37474F;border-bottom:1px solid #E0E7EF;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:12px;background:#FFF3E0;font-weight:600;
				                       color:#E65100;">
				                📅 New Expected Delivery
				            </td>
				            <td style="padding:12px;color:#E65100;font-weight:700;">
				                %s
				            </td>
				        </tr>
				    </table>

				    <div style="background:#FFEBEE;border-left:4px solid #C62828;
				                padding:14px;border-radius:4px;margin-top:16px;">
				        <strong style="color:#C62828;">Note:</strong>
				        <span style="color:#37474F;">
				            We sincerely apologize for the inconvenience.
				            Our team is working hard to deliver your shipment as soon as possible.
				        </span>
				    </div>

				    <p style="color:#78909C;font-size:12px;margin-top:24px;text-align:center;">
				        Track your shipment anytime using ID:
				        <strong style="color:#1565C0;">%s</strong>
				    </p>
				</div>
				""".formatted(shipment.getSenderName(), shipment.getTrackingId(),
				shipment.getCurrentStatus().name().replace("_", " "), shipment.getOrigin(), shipment.getDestination(),
				delayDays, reason, formattedRevised, shipment.getTrackingId());

		// ── RECEIVER EMAIL BODY ───────────────────────────────────
		String receiverBody = """
				<div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
				            padding:20px;border:2px solid #C62828;border-radius:12px;">

				    <div style="background:linear-gradient(135deg,#C62828,#E53935);
				                padding:24px;border-radius:8px;text-align:center;margin-bottom:24px;">
				        <h1 style="color:white;margin:0;font-size:24px;">⚠️ Delivery Delay Alert</h1>
				        <p style="color:rgba(255,255,255,0.9);margin:8px 0 0;font-size:14px;">
				            Your package delivery has been rescheduled
				        </p>
				    </div>

				    <p style="color:#37474F;font-size:15px;">
				        Dear <strong>%s</strong>, we're sorry to let you know that your
				        incoming package has been delayed.
				    </p>

				    <div style="background:#E8F0FE;border-left:4px solid #1565C0;
				                padding:16px;border-radius:4px;margin:16px 0;">
				        <p style="margin:0;font-size:13px;color:#78909C;">Tracking ID</p>
				        <p style="margin:4px 0 0;font-size:22px;font-weight:700;
				                   color:#1565C0;letter-spacing:2px;">%s</p>
				    </div>

				    <table style="width:100%%;border-collapse:collapse;margin:16px 0;">
				        <tr>
				            <td style="padding:12px;background:#F4F7FB;font-weight:600;
				                       color:#37474F;width:40%%;border-bottom:1px solid #E0E7EF;">
				                Sent By
				            </td>
				            <td style="padding:12px;color:#0D1B2A;border-bottom:1px solid #E0E7EF;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:12px;background:#F4F7FB;font-weight:600;
				                       color:#37474F;border-bottom:1px solid #E0E7EF;">
				                From → To
				            </td>
				            <td style="padding:12px;color:#0D1B2A;border-bottom:1px solid #E0E7EF;">
				                %s → %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:12px;background:#FFEBEE;font-weight:600;
				                       color:#C62828;border-bottom:1px solid #E0E7EF;">
				                ⏱ Delayed By
				            </td>
				            <td style="padding:12px;color:#C62828;font-weight:700;
				                       border-bottom:1px solid #E0E7EF;">
				                %d day(s)
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:12px;background:#F4F7FB;font-weight:600;
				                       color:#37474F;border-bottom:1px solid #E0E7EF;">
				                Reason
				            </td>
				            <td style="padding:12px;color:#37474F;border-bottom:1px solid #E0E7EF;">
				                %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:12px;background:#FFF3E0;font-weight:600;
				                       color:#E65100;">
				                📅 New Expected Delivery
				            </td>
				            <td style="padding:12px;color:#E65100;font-weight:700;">
				                %s
				            </td>
				        </tr>
				    </table>

				    <div style="background:#FFF3E0;border-left:4px solid #F57C00;
				                padding:14px;border-radius:4px;margin-top:16px;">
				        <strong style="color:#F57C00;">📞 Please note:</strong>
				        <span style="color:#37474F;">
				            Ensure someone is available to receive the package
				            on the new delivery date.
				        </span>
				    </div>

				    <p style="color:#78909C;font-size:12px;margin-top:24px;text-align:center;">
				        Track your shipment anytime using ID:
				        <strong style="color:#1565C0;">%s</strong>
				    </p>
				</div>
				""".formatted(shipment.getReceiverName(), shipment.getTrackingId(), shipment.getSenderName(),
				shipment.getOrigin(), shipment.getDestination(), delayDays, reason, formattedRevised,
				shipment.getTrackingId());

		// ── SEND ──────────────────────────────────────────────────
//        sendEmail(creatorEmail, subject, senderBody);

		if (shipment.getReceiverEmail() != null && !shipment.getReceiverEmail().isBlank()) {
			sendEmail(shipment.getReceiverEmail(), subject, receiverBody);
			logger.info("✅ Delay notification also sent to receiver: {}", shipment.getReceiverEmail());
		}
	}

//    @Async
//   
//    public void sendDelayNotificationEmail(Shipment shipment,
//                                           String reason,
//                                           LocalDate revisedDate,
//                                           int delayDays) {
//        String subject = "⚠️ Delivery Delay Alert — "
//                + shipment.getTrackingId();
//        String body = """
//            ...
//            """.formatted(
//                shipment.getReceiverName(),
//                shipment.getTrackingId(),
//                shipment.getCurrentStatus().name()
//                        .replace("_", " "),
//                delayDays,
//                reason,
//                revisedDate.toString(),
//                shipment.getOrigin(),
//                shipment.getDestination()
//        );
//
//        // Send to sender/creator
//        sendEmail(shipment.getCreatedBy().getEmail(), subject, body);
//
//        // ✅ Also send to receiver
//        if (shipment.getReceiverEmail() != null &&
//                !shipment.getReceiverEmail().isBlank()) {
//            sendEmail(shipment.getReceiverEmail(), subject, body);
//            logger.info("✅ Delay notification also sent to receiver: {}",
//                    shipment.getReceiverEmail());
//        }
//    }

	// ── 6. SHIPMENT DELETED EMAIL ─────────────────────────────
	@Async
	@Transactional
	public void sendShipmentDeletedEmail(Shipment shipment, String adminEmail) {
		String subject = "Shipment Deleted — " + shipment.getTrackingId();
		String body = """
				<div style="font-family:Arial,sans-serif;
				            max-width:600px;margin:auto;
				            padding:20px;
				            border:1px solid #E0E7EF;
				            border-radius:12px;">
				    <h2 style="color:#C62828;">
				        🗑️ Shipment Record Deleted
				    </h2>
				    <p style="color:#37474F;">
				        The following shipment has been deleted.
				    </p>
				    <table style="width:100%%;
				                  border-collapse:collapse;
				                  margin:16px 0;">
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;
				                       width:40%%;">
				                Tracking ID
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Sender
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Receiver
				            </td>
				            <td style="padding:10px;">%s</td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Route
				            </td>
				            <td style="padding:10px;">
				                %s → %s
				            </td>
				        </tr>
				        <tr>
				            <td style="padding:10px;
				                       background:#F4F7FB;
				                       font-weight:600;">
				                Deleted At
				            </td>
				            <td style="padding:10px;
				                       color:#C62828;">
				                %s
				            </td>
				        </tr>
				    </table>
				</div>
				""".formatted(shipment.getTrackingId(), shipment.getSenderName(), shipment.getReceiverName(),
				shipment.getOrigin(), shipment.getDestination(), java.time.LocalDateTime.now().toString());
		sendEmail(adminEmail, subject, body);
	}

	// ── HELPER METHODS ────────────────────────────────────────
	private String getSubjectForStatus(ShipmentStatus status, String trackingId) {
		return switch (status) {
		case DISPATCHED -> "Your Shipment is Dispatched 🚚 — " + trackingId;
		case IN_TRANSIT -> "Your Shipment is In Transit 🛣️ — " + trackingId;
		case OUT_FOR_DELIVERY -> "Out for Delivery Today 🏠 — " + trackingId;
		case DELIVERED -> "Shipment Delivered Successfully ✅ — " + trackingId;
		case FAILED -> "Shipment Delivery Failed ❌ — " + trackingId;
		default -> "Shipment Update — " + trackingId;
		};
	}

	private String getColorForStatus(ShipmentStatus status) {
		return switch (status) {
		case DISPATCHED -> "#F57C00";
		case IN_TRANSIT -> "#1565C0";
		case OUT_FOR_DELIVERY -> "#6A1B9A";
		case DELIVERED -> "#00897B";
		case FAILED -> "#C62828";
		default -> "#37474F";
		};
	}

	private String getEmojiForStatus(ShipmentStatus status) {
		return switch (status) {
		case DISPATCHED -> "🚚";
		case IN_TRANSIT -> "🛣️";
		case OUT_FOR_DELIVERY -> "🏠";
		case DELIVERED -> "✅";
		case FAILED -> "❌";
		default -> "📦";
		};
	}

	private String getExtraMessageForStatus(ShipmentStatus status) {
		return switch (status) {
		case DELIVERED -> """
				<div style="background:#E8F5E9;
				            border-left:4px solid #00897B;
				            padding:16px;border-radius:4px;
				            margin:16px 0;">
				    <p style="margin:0;color:#00897B;
				               font-weight:600;">
				        🎉 Your shipment has been delivered!
				        Thank you for using LogiTrack.
				    </p>
				</div>
				""";
		case FAILED -> """
				<div style="background:#FFEBEE;
				            border-left:4px solid #C62828;
				            padding:16px;border-radius:4px;
				            margin:16px 0;">
				    <p style="margin:0;color:#C62828;
				               font-weight:600;">
				        We're sorry your delivery failed.
				        Our team will contact you shortly.
				    </p>
				</div>
				""";
		case OUT_FOR_DELIVERY -> """
				<div style="background:#F3E5F5;
				            border-left:4px solid #6A1B9A;
				            padding:16px;border-radius:4px;
				            margin:16px 0;">
				    <p style="margin:0;color:#6A1B9A;
				               font-weight:600;">
				        📞 Please ensure someone is available
				        to receive the package today.
				    </p>
				</div>
				""";
		default -> "";
		};
	}

	// ── 6. DELIVERY OTP EMAIL ──────────────────────────────────
	// ── 6. DELIVERY OTP EMAIL ──────────────────────────────────
	@Async
	@Transactional
	public void sendDeliveryOtp(String phone, String email, String otp, String trackingId) {
		String subject = "🔑 Delivery Verification Code — " + trackingId;
		String body = """
				<div style="font-family:Arial,sans-serif; max-width:600px; margin:auto; padding:20px; border:1px solid #E0E7EF; border-radius:12px;">
				    <div style="background:#1565C0; padding:24px; border-radius:8px; text-align:center; margin-bottom:24px;">
				        <h1 style="color:white; margin:0; font-size:22px;">Delivery Verification</h1>
				    </div>
				    <p style="color:#37474F;">Hi,</p>
				    <p style="color:#37474F;">Our delivery partner is at your location with shipment <strong>%s</strong>. Please provide the following OTP to complete the delivery:</p>

				    <div style="background:#F4F7FB; border:2px dashed #1565C0; padding:20px; text-align:center; border-radius:8px; margin:20px 0;">
				        <span style="font-size:32px; font-weight:800; color:#1565C0; letter-spacing:8px;">%s</span>
				    </div>

				    <p style="color:#C62828; font-size:12px; font-weight:600;">⚠️ Important: Do not share this OTP until you have physically received your package.</p>
				    <p style="color:#78909C; font-size:11px; margin-top:20px;">Verification code sent via Email and SMS (%s).</p>
				</div>
				"""
				.formatted(trackingId, otp, phone);

		sendEmail(email, subject, body);
	}

	// ── 7. FINAL DELIVERED CONFIRMATION ───────────────────────
//    @Async
//    @Transactional
//    public void sendDeliveredConfirmationEmail(Shipment shipment) {
//        String subject = "✅ Delivered: Your package has arrived! — " + shipment.getTrackingId();
//        String body = """
//            <div style="font-family:Arial,sans-serif; max-width:600px; margin:auto; padding:20px; border:1px solid #E0E7EF; border-radius:12px;">
//                <div style="background:#2E7D32; padding:24px; border-radius:8px; text-align:center; margin-bottom:24px;">
//                    <h1 style="color:white; margin:0; font-size:24px;">🎉 Package Delivered!</h1>
//                </div>
//                <p style="color:#37474F;">Great news! Your shipment <strong>%s</strong> has been successfully delivered.</p>
//                <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
//                    <tr>
//                        <td style="padding:10px; background:#F4F7FB; font-weight:600; width:40%%;">Delivery Time</td>
//                        <td style="padding:10px;">%s</td>
//                    </tr>
//                    <tr>
//                        <td style="padding:10px; background:#F4F7FB; font-weight:600;">Destination</td>
//                        <td style="padding:10px;">%s</td>
//                    </tr>
//                </table>
//                <p style="color:#37474F;">Thank you for choosing <strong>LogiTrack</strong>!</p>
//            </div>
//            """.formatted(
//                shipment.getTrackingId(),
//                java.time.LocalDateTime.now().toString(),
//                shipment.getDestination()
//        );
//
//        sendEmail(shipment.getCreatedBy().getEmail(), subject, body);
//        if (shipment.getReceiverEmail() != null) {
//            sendEmail(shipment.getReceiverEmail(), subject, body);
//        }
//    }

	@Async
	@Transactional
	public void sendDeliveredConfirmationEmail(Shipment shipment) {
		String subject = "✅ Delivered: " + shipment.getTrackingId();
		String body = """
				<div style="font-family:Arial,sans-serif; max-width:600px; margin:auto; padding:20px; border:1px solid #E0E7EF; border-radius:12px;">
				    <div style="background:#2E7D32; padding:24px; border-radius:8px; text-align:center; margin-bottom:24px;">
				        <h1 style="color:white; margin:0; font-size:24px;">📦 Package Delivered!</h1>
				    </div>
				    <p style="color:#37474F;">Your shipment <strong>%s</strong> has been successfully delivered.</p>
				    <p style="color:#37474F;"><strong>Time:</strong> %s</p>
				    <p style="color:#37474F;">Thank you for using LogiTrack!</p>
				</div>
				"""
				.formatted(shipment.getTrackingId(), shipment.getDeliveredAt());

		sendEmail(shipment.getReceiverEmail(), subject, body);
	}
}
