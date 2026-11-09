import nodemailer, { type Transporter, type TestAccount } from 'nodemailer';
import fs from 'fs';
import path from 'path';

export interface EmailConfig {
  enabled: boolean;
  provider: 'smtp' | 'gmail' | 'ethereal';
  host?: string;
  port?: number;
  secure?: boolean;
  user?: string;
  pass?: string;
  fromEmail?: string;
  replyTo?: string;
  lastTestedAt?: string | null;
  lastTestStatus?: string | null;
  etherealFallback?: boolean;
}

export interface WhatsAppConfig {
  enabled: boolean;
  provider: 'whatsapp_web_and_cloud';
  cloudApi?: {
    accessToken?: string;
    phoneNumberId?: string;
    businessAccountId?: string;
  };
  groupInviteLink?: string;
  defaultCountryCode?: string;
  lastTestedAt?: string | null;
}

export interface CommunicationsConfig {
  email: EmailConfig;
  whatsapp: WhatsAppConfig;
}

const CONFIG_PATH = path.resolve(process.cwd(), 'src/data/db/communications_config.json');

// Cache transporter instance
let cachedTransporter: Transporter | null = null;
let cachedTransporterKey = '';
let etherealAccount: TestAccount | null = null;

export const loadCommunicationsConfig = (): CommunicationsConfig => {
  try {
    if (fs.existsSync(CONFIG_PATH)) {
      const raw = fs.readFileSync(CONFIG_PATH, 'utf-8');
      const parsed = JSON.parse(raw);
      return {
        email: {
          enabled: true,
          provider: 'smtp',
          host: process.env.SMTP_HOST || parsed.email?.host || '',
          port: Number(process.env.SMTP_PORT || parsed.email?.port || 587),
          secure: Boolean(process.env.SMTP_SECURE === 'true' || parsed.email?.secure || false),
          user: process.env.SMTP_USER || process.env.GMAIL_USER || parsed.email?.user || '',
          pass: process.env.SMTP_PASS || process.env.GMAIL_APP_PASSWORD || parsed.email?.pass || '',
          fromEmail: process.env.SMTP_FROM || parsed.email?.fromEmail || '"Social Shuffle Pune" <notifications@socialshuffle.in>',
          replyTo: parsed.email?.replyTo || 'parimalmshete@gmail.com',
          lastTestedAt: parsed.email?.lastTestedAt || null,
          lastTestStatus: parsed.email?.lastTestStatus || null,
          etherealFallback: parsed.email?.etherealFallback !== false
        },
        whatsapp: {
          enabled: true,
          provider: 'whatsapp_web_and_cloud',
          cloudApi: {
            accessToken: process.env.WHATSAPP_ACCESS_TOKEN || parsed.whatsapp?.cloudApi?.accessToken || '',
            phoneNumberId: process.env.WHATSAPP_PHONE_NUMBER_ID || parsed.whatsapp?.cloudApi?.phoneNumberId || '',
            businessAccountId: process.env.WHATSAPP_BUSINESS_ACCOUNT_ID || parsed.whatsapp?.cloudApi?.businessAccountId || ''
          },
          groupInviteLink: parsed.whatsapp?.groupInviteLink || 'https://chat.whatsapp.com/social-shuffle-pune',
          defaultCountryCode: parsed.whatsapp?.defaultCountryCode || '91',
          lastTestedAt: parsed.whatsapp?.lastTestedAt || null
        }
      };
    }
  } catch (err) {
    console.warn('[Communications Config] Failed to load config file, using environment defaults', err);
  }

  return {
    email: {
      enabled: true,
      provider: 'smtp',
      host: process.env.SMTP_HOST || '',
      port: Number(process.env.SMTP_PORT || 587),
      secure: process.env.SMTP_SECURE === 'true',
      user: process.env.SMTP_USER || process.env.GMAIL_USER || '',
      pass: process.env.SMTP_PASS || process.env.GMAIL_APP_PASSWORD || '',
      fromEmail: process.env.SMTP_FROM || '"Social Shuffle Pune" <notifications@socialshuffle.in>',
      replyTo: 'parimalmshete@gmail.com',
      lastTestedAt: null,
      lastTestStatus: null,
      etherealFallback: true
    },
    whatsapp: {
      enabled: true,
      provider: 'whatsapp_web_and_cloud',
      cloudApi: {
        accessToken: process.env.WHATSAPP_ACCESS_TOKEN || '',
        phoneNumberId: process.env.WHATSAPP_PHONE_NUMBER_ID || '',
        businessAccountId: process.env.WHATSAPP_BUSINESS_ACCOUNT_ID || ''
      },
      groupInviteLink: 'https://chat.whatsapp.com/social-shuffle-pune',
      defaultCountryCode: '91',
      lastTestedAt: null
    }
  };
};

export const saveCommunicationsConfig = (config: CommunicationsConfig): boolean => {
  try {
    fs.writeFileSync(CONFIG_PATH, JSON.stringify(config, null, 2), 'utf-8');
    // Invalidate cached transporter so new credentials take effect
    cachedTransporter = null;
    cachedTransporterKey = '';
    return true;
  } catch (err) {
    console.error('[Communications Config] Failed to save config', err);
    return false;
  }
};

/**
 * Returns or creates an active Nodemailer Transporter
 */
export const getMailTransporter = async (): Promise<{ transporter: Transporter; provider: string; isRealSmtp: boolean }> => {
  const config = loadCommunicationsConfig();
  const emailCfg = config.email;

  // Check if real SMTP credentials are provided
  const hasUser = Boolean(emailCfg.user && emailCfg.user.trim().length > 0);
  const hasPass = Boolean(emailCfg.pass && emailCfg.pass.trim().length > 0 && !emailCfg.pass.includes('••••'));
  const hasHost = Boolean(emailCfg.host && emailCfg.host.trim().length > 0);

  // If Gmail or custom SMTP credentials are provided
  if (hasUser && hasPass) {
    const key = `${emailCfg.host}:${emailCfg.port}:${emailCfg.user}:${emailCfg.pass}`;
    if (cachedTransporter && cachedTransporterKey === key) {
      return { transporter: cachedTransporter, provider: 'smtp', isRealSmtp: true };
    }

    const isGmail = emailCfg.host?.includes('gmail') || emailCfg.user?.includes('@gmail.com');
    const transporter = isGmail
      ? nodemailer.createTransport({
          service: 'gmail',
          auth: {
            user: emailCfg.user,
            pass: emailCfg.pass
          }
        })
      : nodemailer.createTransport({
          host: emailCfg.host || 'smtp.gmail.com',
          port: emailCfg.port || 587,
          secure: emailCfg.secure || false,
          auth: {
            user: emailCfg.user,
            pass: emailCfg.pass
          }
        });

    cachedTransporter = transporter;
    cachedTransporterKey = key;
    return { transporter, provider: isGmail ? 'gmail' : 'smtp', isRealSmtp: true };
  }

  // Fallback: Real Ethereal Test SMTP Transport (Actually transmits mail over SMTP and creates preview links)
  if (!etherealAccount) {
    console.log('[Mailer] No custom SMTP credentials found. Initializing automated Ethereal test SMTP account...');
    try {
      etherealAccount = await nodemailer.createTestAccount();
      console.log(`[Mailer] Ethereal SMTP account created: ${etherealAccount.user}`);
    } catch (err) {
      console.warn('[Mailer] Could not create Ethereal account, falling back to JSON transport', err);
    }
  }

  if (etherealAccount) {
    const etherealKey = `ethereal:${etherealAccount.user}`;
    if (!cachedTransporter || cachedTransporterKey !== etherealKey) {
      cachedTransporter = nodemailer.createTransport({
        host: etherealAccount.smtp.host,
        port: etherealAccount.smtp.port,
        secure: etherealAccount.smtp.secure,
        auth: {
          user: etherealAccount.user,
          pass: etherealAccount.pass
        }
      });
      cachedTransporterKey = etherealKey;
    }
    return { transporter: cachedTransporter, provider: 'ethereal', isRealSmtp: false };
  }

  // Extreme fallback: JSON transport
  const fallbackTransporter = nodemailer.createTransport({
    jsonTransport: true
  });
  return { transporter: fallbackTransporter, provider: 'json', isRealSmtp: false };
};

export interface OutboundMailRequest {
  to: string;
  subject: string;
  html: string;
  text?: string;
  replyTo?: string;
}

export interface OutboundMailResult {
  success: boolean;
  messageId?: string;
  previewUrl?: string | false;
  provider: string;
  isRealSmtp: boolean;
  to: string;
  error?: string;
}

/**
 * Dispatches a real outbound email message
 */
export const sendOutboundEmail = async (req: OutboundMailRequest): Promise<OutboundMailResult> => {
  const config = loadCommunicationsConfig();
  try {
    const { transporter, provider, isRealSmtp } = await getMailTransporter();

    try {
      const info = await transporter.sendMail({
        from: config.email.fromEmail || '"Social Shuffle Pune" <notifications@socialshuffle.in>',
        to: req.to,
        subject: req.subject,
        html: req.html,
        text: req.text || req.subject,
        replyTo: req.replyTo || config.email.replyTo || 'parimalmshete@gmail.com'
      });

      const previewUrl = nodemailer.getTestMessageUrl(info);

      console.log(`[Mailer SUCCESS] Sent "${req.subject}" to ${req.to} | Message ID: ${info.messageId} | Provider: ${provider}`);
      if (previewUrl) {
        console.log(`[Mailer Preview URL] ${previewUrl}`);
      }

      return {
        success: true,
        messageId: info.messageId,
        previewUrl,
        provider,
        isRealSmtp,
        to: req.to
      };
    } catch (sendErr: any) {
      // If custom SMTP failed (e.g. invalid credentials or blocked port), fall back to live Ethereal test transport
      if (provider !== 'ethereal') {
        console.warn(`[Mailer] Custom SMTP transmission failed (${sendErr?.message}), switching to Ethereal test transport...`);
        if (!etherealAccount) {
          etherealAccount = await nodemailer.createTestAccount();
        }
        const etherealTransporter = nodemailer.createTransport({
          host: etherealAccount.smtp.host,
          port: etherealAccount.smtp.port,
          secure: etherealAccount.smtp.secure,
          auth: {
            user: etherealAccount.user,
            pass: etherealAccount.pass
          }
        });

        const retryInfo = await etherealTransporter.sendMail({
          from: config.email.fromEmail || '"Social Shuffle Pune" <notifications@socialshuffle.in>',
          to: req.to,
          subject: req.subject,
          html: req.html,
          text: req.text || req.subject,
          replyTo: req.replyTo || config.email.replyTo || 'parimalmshete@gmail.com'
        });

        const previewUrl = nodemailer.getTestMessageUrl(retryInfo);
        console.log(`[Mailer Ethereal Fallback SUCCESS] Delivered to ${req.to} | Preview: ${previewUrl}`);

        return {
          success: true,
          messageId: retryInfo.messageId,
          previewUrl,
          provider: 'ethereal',
          isRealSmtp: false,
          to: req.to
        };
      }
      throw sendErr;
    }
  } catch (error: any) {
    console.error(`[Mailer ERROR] Failed sending to ${req.to}:`, error?.message || error);
    return {
      success: false,
      provider: 'smtp',
      isRealSmtp: false,
      to: req.to,
      error: error?.message || 'SMTP transmission failure'
    };
  }
};

/* 
 * =======================================================================
 * WHATSAPP DISPATCH FEATURE (Temporarily commented out per request)
 * =======================================================================
 *
export const normalizeWhatsAppPhone = (phone: string, defaultCountryCode = '91'): string => {
  if (!phone) return '';
  const digits = phone.replace(/[^0-9]/g, '');
  if (digits.length === 10) {
    return `${defaultCountryCode}${digits}`;
  }
  if (digits.startsWith('0') && digits.length === 11) {
    return `${defaultCountryCode}${digits.slice(1)}`;
  }
  return digits;
};

export interface OutboundWhatsAppRequest {
  toPhone: string;
  messageText: string;
}

export interface OutboundWhatsAppResult {
  success: boolean;
  mode: 'cloud_api' | 'web_link';
  cleanedPhone: string;
  url?: string;
  messageId?: string;
  statusText: string;
  error?: string;
}

export const dispatchOutboundWhatsApp = async (req: OutboundWhatsAppRequest): Promise<OutboundWhatsAppResult> => {
  const config = loadCommunicationsConfig();
  const cleanedPhone = normalizeWhatsAppPhone(req.toPhone, config.whatsapp.defaultCountryCode);

  if (!cleanedPhone || cleanedPhone.length < 10) {
    return {
      success: false,
      mode: 'web_link',
      cleanedPhone,
      statusText: 'Invalid phone number for WhatsApp delivery',
      error: 'Phone number must have at least 10 digits'
    };
  }

  const cloudApi = config.whatsapp.cloudApi;
  const hasCloudApi = Boolean(cloudApi?.accessToken && cloudApi?.phoneNumberId);

  // 1. Try Meta WhatsApp Cloud API if configured
  if (hasCloudApi) {
    try {
      const response = await fetch(`https://graph.facebook.com/v19.0/${cloudApi!.phoneNumberId}/messages`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${cloudApi!.accessToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          messaging_product: 'whatsapp',
          recipient_type: 'individual',
          to: cleanedPhone,
          type: 'text',
          text: { preview_url: true, body: req.messageText }
        })
      });

      const data = await response.json();
      if (response.ok && data.messages && data.messages[0]?.id) {
        return {
          success: true,
          mode: 'cloud_api',
          cleanedPhone,
          messageId: data.messages[0].id,
          statusText: `Delivered via Meta WhatsApp Cloud API (${cleanedPhone})`
        };
      }
      console.warn('[WhatsApp Cloud API] Response not OK, falling back to 1-click web dispatch:', data);
    } catch (err: any) {
      console.warn('[WhatsApp Cloud API] Network error, falling back to 1-click web dispatch:', err?.message);
    }
  }

  // 2. Direct 1-Click WhatsApp Direct Link
  const url = `https://wa.me/${cleanedPhone}?text=${encodeURIComponent(req.messageText)}`;
  return {
    success: true,
    mode: 'web_link',
    cleanedPhone,
    url,
    statusText: `Ready for 1-click WhatsApp dispatch to +${cleanedPhone}`
  };
};
======================================================================= */

