package com.taobao.tddl.atom.securety;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface TPasswordCoder {

    /**
     * @param encKey 加密串
     * @param secret 密文
     */
    public abstract String encode(String encKey, String secret) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException;

    /**
     * @param encKey 加密串
     * @param secret 密文
     */
    public abstract String encode(String secret) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException;

    /**
     * @param encKey 加密串
     * @param secret 密文
     */
    public abstract String decode(String encKey, String secret) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException;

    /**
     * @param encKey 加密串
     * @param secret 密文
     */
    public abstract char[] decode(String secret) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException;

}
