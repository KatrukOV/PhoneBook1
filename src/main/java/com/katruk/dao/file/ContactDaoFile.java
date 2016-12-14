package com.katruk.dao.file;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katruk.dao.ContactDao;
import com.katruk.domain.entity.Address;
import com.katruk.domain.entity.Contact;
import com.katruk.domain.entity.Person;
import com.katruk.domain.entity.User;
import com.katruk.domain.entity.json.ContactJson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Profile(value = "file")
public class ContactDaoFile implements ContactDao {

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private UserDaoFile userDaoFile;

  private File jsonFile;

  protected File getJsonFile() {
    try {
      this.jsonFile = new File("src/main/resources/json/contact.json");
      if (!jsonFile.exists()) {
        this.jsonFile.createNewFile();
      }
    } catch (IOException e) {
      //todo log and throw
      e.printStackTrace();
    }
    return jsonFile;
  }

  @Override
  public Optional<Contact> getContactById(Long contactId) {

    List<ContactJson> lists = getAll();
    for (ContactJson element : lists) {
      if (element.getContactId().equals(contactId)) {
        Contact contact = createContact(element);
        return Optional.of(contact);
      }
    }
    return Optional.empty();
  }

  public Set<Contact> getContactByUserId(Long userId) {
    System.out.println(">>> getContactByUserId userId=" + userId);
    List<ContactJson> list = getAll();
    Set<Contact> result = new HashSet<>();
    for (ContactJson contactJson : list) {
      if (contactJson.getUserId().equals(userId)) {
        Contact contact = createContact(contactJson);
        result.add(contact);
      }
    }
    return result;
  }

  @Override
  public Contact saveAndFlush(Contact contact) {

    List<ContactJson> list = getAll();
    ContactJson contactJson = createContactJson(contact);
    System.out.println(">>> saveAndFlush size=" + list.size() + "  list=" + list);

    boolean isUnique = true;
    for (ContactJson element : list) {
      System.out.println(
          ">>> Unique ??? element.getContactId()=" + element.getContactId() + "  contact.getId()="
          + contact.getId());
      if (element.getContactId().equals(contact.getId())) {
        System.out.println(">>> NOT Unique ");
        isUnique = false;
      }
    }
    if (isUnique) {
      contactJson.setContactId(UUID.randomUUID().getLeastSignificantBits());
    } else {
      list = remove(contactJson.getContactId());
    }
    System.out.println(">>> saveAndFlush before add size=" + list.size() + "  list=" + list);
    try {
      list.add(contactJson);
      objectMapper.writeValue(getJsonFile(), list);
    } catch (IOException e) {
      //// TODO: log + exc
      e.printStackTrace();
    }
    System.out.println(">>> saveAndFlush after add size=" + list.size() + "  list=" + list);
    return contact;
  }


  @Override
  public void delete(Long contactId) {
    remove(contactId);
  }

  private List<ContactJson> remove(Long contactId) {
    List<ContactJson> list = getAll();
    Contact contact = getContactById(contactId)
        .orElseThrow(() -> new NoSuchElementException("Element not found"));
    ContactJson contactJson = createContactJson(contact);
    System.out.println(">>>>>>>>>>>> to remove List=" + list);
    System.out.println(">>>>>>>>>>>> to remove List size=" + list.size());
    System.out.println(">>>>>>>>>>>> to remove contactJson=" + contactJson);
    System.out.println(">>>>>>>>>>>> to index contactJson=" + list.indexOf(contactJson));
    System.out.println(">>>>>>>>>>>> to =" + contactJson);

    list = removeFromList(list, contactJson);

    System.out.println(">>>>>>>>>>>> !!!!!!!!!!! after remove List=" + list);
    System.out.println(">>>>>>>>>>>> after remove List size=" + list.size());
    try {
      objectMapper.writeValue(getJsonFile(), list);
    } catch (IOException e) {
      e.printStackTrace();
    }
    List<ContactJson> list1 = getAll();
    System.out.println(">>> after DELETE list" + list1);
    return list;
  }

  private List<ContactJson> removeFromList(List<ContactJson> list, ContactJson contactJson) {
    List<ContactJson> result = new ArrayList<>();
    for (ContactJson element : list) {
      if (!element.getContactId().equals(contactJson.getContactId())) {
        result.add(element);
      }
    }
    return result;
  }

  private ContactJson createContactJson(Contact contact) {
    System.out.println(">>> createContactJson 1 contact=" + contact);
    System.out.println(">>> createContactJson 1 contact.user=" + contact.getUser());
    ContactJson contactJson = new ContactJson();
    contactJson.setContactId(contact.getId());
    contactJson.setUserId(contact.getUser().getId());
    contactJson.setLastName(contact.getPerson().getLastName());
    contactJson.setName(contact.getPerson().getName());
    contactJson.setPatronymic(contact.getPerson().getPatronymic());
    contactJson.setMobilePhone(contact.getMobilePhone());
    contactJson.setHomePhone(contact.getHomePhone());
    contactJson.setEmail(contact.getEmail());
    if (nonNull(contact.getAddress())) {
      contactJson.setCity(contact.getAddress().getCity());
      contactJson.setStreet(contact.getAddress().getStreet());
      contactJson.setBuilding(contact.getAddress().getBuilding());
      contactJson.setApartment(contact.getAddress().getApartment());
    }
    System.out.println(">>> createContactJson 2 contactJson=" + contactJson);
    return contactJson;
  }

  private Contact createContact(ContactJson contactJson) {

    System.out.println(">>> createContact 1 contactJson=" + contactJson);
    Contact contact = new Contact();
    User user = this.userDaoFile.getUserById(contactJson.getUserId())
        .orElseThrow(() -> new NoSuchElementException("Element not found"));
    contact.setUser(user);

    contact.setId(contactJson.getContactId());
    Person person = new Person();
    person.setLastName(contactJson.getLastName());
    person.setName(contactJson.getName());
    person.setPatronymic(contactJson.getPatronymic());
    contact.setPerson(person);
    Address address = new Address();
    address.setCity(contactJson.getCity());
    address.setStreet(contactJson.getStreet());
    address.setBuilding(contactJson.getBuilding());
    address.setApartment(contactJson.getApartment());
    contact.setAddress(address);
    contact.setMobilePhone(contactJson.getMobilePhone());
    contact.setHomePhone(contactJson.getHomePhone());
    contact.setEmail(contactJson.getEmail());
    System.out.println(">>> createContact 2 contact=" + contact);
    return contact;
  }

  public List<ContactJson> getAll() {
    System.out.println(">>> getAll ContactJson start");
    List<ContactJson> list = new ArrayList<>();

    File jsonFile = getJsonFile();

    if (jsonFile.exists() && !jsonFile.isDirectory()) {
      try {
        list = objectMapper.readValue(jsonFile, new TypeReference<List<ContactJson>>() {
        });
      } catch (IOException e) {
        // TODO:  log
        e.printStackTrace();
      }
    }
    for (ContactJson con : list) {
      System.out.println(">>> getAll contact=  " + con);
    }
    return list;
  }
}
