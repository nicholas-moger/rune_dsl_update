package chaos.s01.a2qual;

import chaos.s01.a2qual.meta.C1BaseMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Root of the S01 inheritance chain.
 * @version 1.0.0
 */
@RosettaDataType(value="C1Base", builder=C1Base.C1BaseBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C1Base", model="chaos", builder=C1Base.C1BaseBuilderImpl.class, version="1.0.0")
public interface C1Base extends RosettaModelObject {

	C1BaseMeta metaData = new C1BaseMeta();

	/*********************** Getter Methods  ***********************/
	String getBaseId();
	String getNote();

	/*********************** Build Methods  ***********************/
	C1Base build();
	
	C1Base.C1BaseBuilder toBuilder();
	
	static C1Base.C1BaseBuilder builder() {
		return new C1Base.C1BaseBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C1Base> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C1Base> getType() {
		return C1Base.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("baseId"), String.class, getBaseId(), this);
		processor.processBasic(path.newSubPath("note"), String.class, getNote(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C1BaseBuilder extends C1Base, RosettaModelObjectBuilder {
		C1Base.C1BaseBuilder setBaseId(String baseId);
		C1Base.C1BaseBuilder setNote(String note);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("baseId"), String.class, getBaseId(), this);
			processor.processBasic(path.newSubPath("note"), String.class, getNote(), this);
		}
		

		C1Base.C1BaseBuilder prune();
	}

	/*********************** Immutable Implementation of C1Base  ***********************/
	class C1BaseImpl implements C1Base {
		private final String baseId;
		private final String note;
		
		protected C1BaseImpl(C1Base.C1BaseBuilder builder) {
			this.baseId = builder.getBaseId();
			this.note = builder.getNote();
		}
		
		@Override
		@RosettaAttribute("baseId")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("baseId")
		public String getBaseId() {
			return baseId;
		}
		
		@Override
		@RosettaAttribute("note")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("note")
		public String getNote() {
			return note;
		}
		
		@Override
		public C1Base build() {
			return this;
		}
		
		@Override
		public C1Base.C1BaseBuilder toBuilder() {
			C1Base.C1BaseBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C1Base.C1BaseBuilder builder) {
			ofNullable(getBaseId()).ifPresent(builder::setBaseId);
			ofNullable(getNote()).ifPresent(builder::setNote);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1Base _that = getType().cast(o);
		
			if (!Objects.equals(baseId, _that.getBaseId())) return false;
			if (!Objects.equals(note, _that.getNote())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (baseId != null ? baseId.hashCode() : 0);
			_result = 31 * _result + (note != null ? note.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1Base {" +
				"baseId=" + this.baseId + ", " +
				"note=" + this.note +
			'}';
		}
	}

	/*********************** Builder Implementation of C1Base  ***********************/
	class C1BaseBuilderImpl implements C1Base.C1BaseBuilder {
	
		protected String baseId;
		protected String note;
		
		@Override
		@RosettaAttribute("baseId")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("baseId")
		public String getBaseId() {
			return baseId;
		}
		
		@Override
		@RosettaAttribute("note")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("note")
		public String getNote() {
			return note;
		}
		
		@RosettaAttribute("baseId")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("baseId")
		@Override
		public C1Base.C1BaseBuilder setBaseId(String _baseId) {
			this.baseId = _baseId == null ? null : _baseId;
			return this;
		}
		
		@RosettaAttribute("note")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("note")
		@Override
		public C1Base.C1BaseBuilder setNote(String _note) {
			this.note = _note == null ? null : _note;
			return this;
		}
		
		@Override
		public C1Base build() {
			return new C1Base.C1BaseImpl(this);
		}
		
		@Override
		public C1Base.C1BaseBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Base.C1BaseBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBaseId()!=null) return true;
			if (getNote()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Base.C1BaseBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C1Base.C1BaseBuilder o = (C1Base.C1BaseBuilder) other;
			
			
			merger.mergeBasic(getBaseId(), o.getBaseId(), this::setBaseId);
			merger.mergeBasic(getNote(), o.getNote(), this::setNote);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1Base _that = getType().cast(o);
		
			if (!Objects.equals(baseId, _that.getBaseId())) return false;
			if (!Objects.equals(note, _that.getNote())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (baseId != null ? baseId.hashCode() : 0);
			_result = 31 * _result + (note != null ? note.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1BaseBuilder {" +
				"baseId=" + this.baseId + ", " +
				"note=" + this.note +
			'}';
		}
	}
}
