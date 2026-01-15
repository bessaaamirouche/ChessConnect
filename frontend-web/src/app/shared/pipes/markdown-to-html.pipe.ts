import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'markdownToHtml',
  standalone: true
})
export class MarkdownToHtmlPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(markdown: string | null): SafeHtml {
    if (!markdown) return '';

    let html = markdown;

    // Headers
    html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
    html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
    html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');

    // Bold
    html = html.replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>');

    // Italic
    html = html.replace(/\*(.*?)\*/gim, '<em>$1</em>');

    // Code
    html = html.replace(/`(.*?)`/gim, '<code>$1</code>');

    // Unordered lists
    html = html.replace(/^\- (.*$)/gim, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');

    // Ordered lists (simple)
    html = html.replace(/^\d+\. (.*$)/gim, '<li>$1</li>');

    // Line breaks (double newlines = paragraph)
    html = html.replace(/\n\n/gim, '</p><p>');
    html = html.replace(/\n/gim, '<br>');

    // Wrap in paragraph
    if (!html.startsWith('<h') && !html.startsWith('<ul') && !html.startsWith('<ol')) {
      html = '<p>' + html + '</p>';
    }

    // Clean up empty paragraphs
    html = html.replace(/<p><\/p>/gim, '');
    html = html.replace(/<p><h/gim, '<h');
    html = html.replace(/<\/h(\d)><\/p>/gim, '</h$1>');

    return this.sanitizer.bypassSecurityTrustHtml(html);
  }
}
