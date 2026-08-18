package org.example.b3.controller;

import org.example.b3.dto.BookingExtraction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/booking")
public class ChatController {

    private final ChatModel chatModel;

    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/extract")
    public BookingExtraction extractBooking() {
        BeanOutputConverter<BookingExtraction> converter =
                new BeanOutputConverter<>(BookingExtraction.class);

        String formatInstructions = converter.getFormat();

        LocalDate today = LocalDate.of(2026, 7, 17);
        String todayStr = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String promptTemplate = """
                Bạn là một hệ thống bóc tách thông tin đặt phòng khách sạn thông minh.

                ## NHIỆM VỤ
                Phân tích email của khách hàng và trích xuất thông tin đặt phòng cuối cùng (final decision).

                ## QUY TRÌNH XỬ LÝ
                1. Đọc toàn bộ email từ đầu đến cuối.
                2. Nhận diện tất cả các thay đổi/mâu thuẫn mà khách hàng đề cập.
                3. Áp dụng nguyên tắc: KHI CÓ THÔNG TIN MÂU THUẪN, LUÔN CHỌN QUYẾT ĐỊNH CUỐI CÙNG (câu lệnh ở phía sau).
                   - Cụm từ "À mà không", "xin lỗi", "thay đổi", "rút ngắn", "lùi lại", "hủy"... chỉ ra khách hàng đang phủ định thông tin trước đó.
                   - Thông tin mới nhất (ở cuối email hoặc sau từ "nhưng mà", "thay vì", "thay vào đó") là quyết định chính thức.
                4. Nếu email chứa từ ngữ tương đối về thời gian ("ngày mai", "ngày kia", "tuần sau"...),
                   hãy tính toán ngày cụ thể dựa trên mốc thời gian: Hôm nay là ngày: {today}.
                5. Chuyển đổi kết quả sang định dạng JSON được yêu cầu.

                ## VÍ DỤ MINH HỌA
                Email: "Tôi muốn đặt phòng Standard, ừ nhưng thôi nâng lên Deluxe nhé"
                -> roomType = "Deluxe" (chọn quyết định cuối)

                Email: "Check-in ngày mai, nhưng mà thôi lùi lại 1 ngày đi"
                -> checkInDate = "ngày mai + 1 ngày"

                ## KẾT QUẢ
                {formatInstructions}

                ## EMAIL CẦN XỬ LÝ
                {email}
                """;

        String email = """
                Chào lễ tân, tôi tên là Minh. Tôi định đặt phòng Suite cho 3 ngày bắt đầu từ ngày mai.
                À mà không, mai tôi bận đột xuất nên cho tôi check-in lùi lại 1 ngày nhé,
                và tôi rút ngắn chuyến đi xuống còn 2 ngày thôi. Có gì liên hệ lại tôi.
                """;

        String prompt = promptTemplate
                .replace("{today}", todayStr)
                .replace("{formatInstructions}", formatInstructions)
                .replace("{email}", email);

        String response = ChatClient.create(chatModel)
                .prompt(prompt)
                .call()
                .content();

        return converter.convert(response);
    }
}