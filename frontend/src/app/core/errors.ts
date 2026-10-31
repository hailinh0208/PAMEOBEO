import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

const MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: 'Email hoặc mật khẩu không đúng.',
  ACCOUNT_TEMPORARILY_LOCKED: 'Bạn nhập sai quá 5 lần. Tài khoản tạm khóa 15 phút.',
  EMAIL_ALREADY_EXISTS: 'Email này đã được đăng ký.',
  INVALID_REFRESH_TOKEN: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.',
  NOT_ENOUGH_SLOTS: 'Ngày nấu không còn đủ suất.',
  CUTOFF_PASSED: 'Đã qua giờ chốt đơn 18:00.',
  MENU_ITEM_UNAVAILABLE: 'Có món trong giỏ đã ngừng bán.',
  INVALID_IMAGE: 'Ảnh không hợp lệ: chỉ nhận JPG, PNG hoặc WebP, tối đa 2 MB.',
  TOO_MANY_IMAGES: 'Mỗi sản phẩm tối đa 3 ảnh. Xóa bớt một ảnh trước khi thêm.',
  CONCURRENT_UPDATE: 'Dữ liệu vừa được thay đổi ở nơi khác, hãy tải lại trang.',
  SERVICE_BUSY: 'Hệ thống bếp đang bận, vui lòng thử lại sau ít phút.',
  TOO_MANY_JARS: 'Mỗi đơn tối đa 10 hộp.',
  CANCELLATION_NOT_ALLOWED: 'Không thể hủy đơn này nữa.',
  INVALID_CAPACITY: 'Công suất không được nhỏ hơn số hộp đã nhận.',
  INVALID_STATUS_TRANSITION: 'Không thể chuyển trạng thái theo thứ tự này.',
};

export function apiError(err: unknown): ApiError {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as Partial<ApiError> | null;
    if (body && typeof body === 'object' && body.code) {
      return { code: body.code, message: body.message ?? '', suggestedCookingDate: body.suggestedCookingDate };
    }
    if (err.status === 0) return { code: 'NETWORK', message: 'Không kết nối được tới server (gateway :9000).' };
    if (err.status === 401) return { code: 'UNAUTHORIZED', message: 'Bạn cần đăng nhập.' };
    if (err.status === 403) return { code: 'FORBIDDEN', message: 'Bạn không có quyền thực hiện thao tác này.' };
    return { code: 'HTTP_' + err.status, message: err.message };
  }
  return { code: 'UNKNOWN', message: String(err) };
}

/** Friendly Vietnamese message for an error returned by the backend. */
export function errorText(err: unknown): string {
  const e = apiError(err);
  return MESSAGES[e.code] ?? e.message ?? e.code;
}
