import { request } from './request'

const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
const maxSize = 5 * 1024 * 1024

export function validateImageFile(file: File) {
  if (!allowedTypes.includes(file.type)) {
    throw new Error('仅支持 JPG、PNG、GIF、WebP 图片')
  }
  if (file.size > maxSize) {
    throw new Error('单张图片不能超过 5MB')
  }
}

export function uploadImage(file: File, biz = 'item') {
  validateImageFile(file)
  const form = new FormData()
  form.append('file', file)
  form.append('biz', biz)
  return request<string>({
    url: '/api/upload/image',
    method: 'POST',
    data: form,
  })
}

export function uploadImages(files: File[], biz = 'item') {
  files.forEach(validateImageFile)
  const form = new FormData()
  files.forEach((file) => form.append('files', file))
  form.append('biz', biz)
  return request<string[]>({
    url: '/api/upload/multi',
    method: 'POST',
    data: form,
  })
}
